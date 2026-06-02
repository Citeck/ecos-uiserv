package ru.citeck.ecos.uiserv.domain.board.cardorder.service

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.BoardCardOrderDesc
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.BoardInfo
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.CardRec
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.ColumnContent
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.JournalInfo
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.MoveCardAction
import ru.citeck.ecos.uiserv.domain.board.cardorder.repo.BoardCardOrderRepo
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Service
class BoardCardOrderService(
    private val recordsService: RecordsService,
    private val orderRepo: BoardCardOrderRepo,
    private val ecosTypeService: EcosTypeService,
    private val boardService: BoardService,
    private val workspaceService: WorkspaceService
) {

    private val cardsHardCap = 10_000

    /**
     * Board identity stored in the `boardRef` ENTITY_REF attribute: `uiserv/board@<localId>`. This is the
     * board's own reference (carrying the board's own workspace prefix, if any) — independent of the order
     * workspace, which is a separate column. Any incoming boardRef form (bare id, board@id, rboard@id) maps
     * to the same key, matching the canonical key the delete-cleanup listener builds.
     */
    private fun boardKey(boardRef: EntityRef): String = EntityRef.create(Application.NAME, "board", boardRef.getLocalId()).toString()

    /** Order records are workspaceScope=PRIVATE; a blank order workspace maps to the default workspace. */
    private fun normalizeWorkspace(workspace: String): String = workspace.ifBlank { ModelUtils.DEFAULT_WORKSPACE_ID }

    /**
     * Remove a board's ordering records when the board itself is deleted. Order may exist in several
     * (viewing) workspaces independent of the board's own workspace, so cleanup runs across all of them.
     */
    @PostConstruct
    fun registerListeners() {
        boardService.onBoardDeleted { deleted ->
            val localId = workspaceService.addWsPrefixToId(deleted.id, deleted.workspace)
            val key = EntityRef.create(Application.NAME, "board", localId).toString()
            AuthContext.runAsSystem { orderRepo.deleteByBoard(key) }
        }
    }

    // ---- public read API ----

    /** Per-column page request: which column, how many to skip, and the page size. */
    class ColumnPageReq(
        val columnId: String = "",
        val skipCount: Int = 0,
        val maxItems: Int = DEFAULT_MAX_ITEMS
    )

    /**
     * Unified board-cards load. [columnPages] selects which columns to load and how to page each one;
     * when null/empty, every board column is loaded with skip 0 and [DEFAULT_MAX_ITEMS] page size.
     * Each returned [ColumnContent] carries the column's full [ColumnContent.totalCount] and the
     * requested page of card refs in persisted display order. [filter] is AND-ed into every column's
     * card query (e.g. text search, journal filters, a swimlane-row predicate).
     */
    fun getBoardCards(
        boardRef: EntityRef,
        columnPages: List<ColumnPageReq>?,
        filter: Predicate?,
        grouping: String = BoardCardOrderDesc.GROUPING_FLAT,
        defaultMaxItems: Int = DEFAULT_MAX_ITEMS,
        workspace: String = ""
    ): List<ColumnContent> {
        val ws = normalizeWorkspace(workspace)
        val board = resolveBoard(boardRef)
        val boardRefStr = boardKey(boardRef)
        val colById = board.columns.associateBy { it.id }
        val reqs = if (columnPages.isNullOrEmpty()) {
            board.columns.map { ColumnPageReq(it.id, 0, defaultMaxItems) }
        } else {
            columnPages
        }
        val neededCols = reqs.mapNotNull { colById[it.columnId] }.distinctBy { it.id }
        require(neededCols.isNotEmpty()) { "No known columns requested for board $boardRef" }
        val byStatus = loadCardsByStatus(board, neededCols, filter)
        return reqs.mapNotNull { req ->
            val col = colById[req.columnId] ?: return@mapNotNull null
            val ordered = mergeColumn(boardRefStr, ws, grouping, col.id, byStatus[col.id].orEmpty())
            ColumnContent(
                columnId = col.id,
                name = col.name,
                totalCount = ordered.size.toLong(),
                cards = ordered.drop(req.skipCount.coerceAtLeast(0)).take(req.maxItems.coerceAtLeast(0)).map { it.ref }
            )
        }
    }

    // ---- public write API ----

    /**
     * Moves [MoveCardAction.card] into [MoveCardAction.column], positioned right after
     * [MoveCardAction.afterCard] (or on top of the ranked section when afterCard is null/empty).
     * Changing the column also changes the card's `_status`. Returns the assigned rank key.
     */
    @Transactional
    fun moveCard(cfg: MoveCardAction, workspace: String = ""): String = doMoveCard(cfg, normalizeWorkspace(workspace), 0)

    private fun doMoveCard(cfg: MoveCardAction, workspace: String, rebalanceDepth: Int): String {
        require(EntityRef.isNotEmpty(cfg.board)) { "board is required" }
        require(EntityRef.isNotEmpty(cfg.card)) { "card is required" }
        require(cfg.column.isNotBlank()) { "column is required" }

        val board = resolveBoard(cfg.board)
        require(board.columns.any { it.id == cfg.column }) {
            "Unknown column '${cfg.column}' for board ${cfg.board}"
        }

        // 1. status change (delegates write-permission check + side effects to the records layer)
        val currentStatus = recordsService.getAtt(cfg.card, "_status?str").asText()
        if (currentStatus != cfg.column) {
            recordsService.mutate(cfg.card, mapOf("_status" to cfg.column))
        }

        // 2. current cards of the target column (after the status change)
        val cards = loadColumnCards(board, cfg.column)
        val boardRefStr = boardKey(cfg.board)
        val grouping = cfg.grouping
        // Effective rank falls back to the flat ("") order for cards not yet ranked in this grouping,
        // so a freshly-grouped view inherits the flat manual order as its baseline. New keys are
        // written to this grouping, evolving it independently from there.
        val rankByCard = effectiveRankByCard(boardRefStr, workspace, grouping, cfg.column).toMutableMap()

        // 3. fold the unranked prefix (created-desc) into rank keys below the smallest existing key
        val unranked = cards.filter { rankByCard[it.ref.toString()] == null }
            .sortedByDescending { it.created }
        if (unranked.isNotEmpty()) {
            val rankedSorted = cards.filter { rankByCard[it.ref.toString()] != null }
                .sortedWith(compareBy({ rankByCard.getValue(it.ref.toString()) }, { it.ref.toString() }))
            if (rankedSorted.isEmpty()) {
                val seeds = RankKeys.seedSpread(unranked.size)
                unranked.forEachIndexed { index, card ->
                    rankByCard[card.ref.toString()] = seeds[index]
                    orderRepo.upsert(boardRefStr, workspace, grouping, card.ref.toString(), cfg.column, seeds[index])
                }
            } else {
                val firstRankedKey = rankByCard.getValue(rankedSorted.first().ref.toString())
                var lowerKey: String? = null
                for (card in unranked) {
                    val key = RankKeys.between(lowerKey, firstRankedKey)
                    if (key.length > RankKeys.MAX_RANK_LEN) {
                        rebalanceForRetry(rebalanceDepth, boardRefStr, workspace, grouping, cfg.column)
                        return doMoveCard(cfg, workspace, rebalanceDepth + 1)
                    }
                    rankByCard[card.ref.toString()] = key
                    orderRepo.upsert(boardRefStr, workspace, grouping, card.ref.toString(), cfg.column, key)
                    lowerKey = key
                }
            }
        }

        // 4. fully-ranked display order, excluding the moved card
        val displayNow = cards.asSequence()
            .filter { it.ref != cfg.card }
            .sortedWith(compareBy({ rankByCard.getValue(it.ref.toString()) }, { it.ref.toString() }))
            .toList()

        // 5. find insertion neighbours by `afterCard` (afterCard not in column -> treat as top)
        val afterRef = cfg.afterCard?.takeIf { EntityRef.isNotEmpty(it) }
        val afterIdx = if (afterRef == null) -1 else displayNow.indexOfFirst { it.ref == afterRef }
        val prevKey: String? = if (afterIdx >= 0) rankByCard.getValue(displayNow[afterIdx].ref.toString()) else null
        val nextKey: String? = when {
            afterIdx >= 0 && afterIdx + 1 < displayNow.size -> rankByCard.getValue(displayNow[afterIdx + 1].ref.toString())
            afterIdx < 0 && displayNow.isNotEmpty() -> rankByCard.getValue(displayNow[0].ref.toString())
            else -> null
        }

        val newKey = RankKeys.between(prevKey, nextKey)
        if (newKey.length > RankKeys.MAX_RANK_LEN) {
            rebalanceForRetry(rebalanceDepth, boardRefStr, workspace, grouping, cfg.column)
            return doMoveCard(cfg, workspace, rebalanceDepth + 1)
        }
        orderRepo.upsert(boardRefStr, workspace, grouping, cfg.card.toString(), cfg.column, newKey)
        return newKey
    }

    /**
     * Rebalance a column's keys when a freshly computed key overflows [RankKeys.MAX_RANK_LEN], so the
     * caller can retry the move once. Fails fast if a key still overflows after a rebalance already ran.
     */
    private fun rebalanceForRetry(rebalanceDepth: Int, boardKey: String, workspace: String, grouping: String, columnId: String) {
        check(rebalanceDepth < 1) {
            "Rank key still exceeds ${RankKeys.MAX_RANK_LEN} after rebalance " +
                "(board=$boardKey, workspace='$workspace', grouping='$grouping', column=$columnId)"
        }
        rebalanceColumn(boardKey, workspace, grouping, columnId)
    }

    /** Re-spread all ranked cards of (board, workspace, grouping, column) using fresh seed keys, preserving current order. */
    @Transactional
    internal fun rebalanceColumn(boardRef: String, workspace: String, grouping: String, columnId: String) {
        val orderRecs = orderRepo.findByBoardAndColumn(boardRef, workspace, grouping, columnId)
            .sortedWith(compareBy({ it.rankKey }, { it.cardRef }))
        if (orderRecs.isEmpty()) return
        val seeds = RankKeys.seedSpread(orderRecs.size)
        orderRecs.forEachIndexed { index, rec -> orderRepo.upsert(boardRef, workspace, grouping, rec.cardRef, columnId, seeds[index]) }
    }

    // ---- internals ----

    /** Resolves a board (saved OR type$ virtual) via the existing `rboard` source. */
    fun resolveBoard(boardRef: EntityRef): ResolvedBoard {
        val localId = boardRef.getLocalId()
        val info = recordsService.getAtts(
            EntityRef.create(Application.NAME, "rboard", localId),
            BoardInfo::class.java
        )
        require(info.columns.isNotEmpty()) { "Board $boardRef has no columns" }
        return ResolvedBoard(boardRef, info.typeRef, info.journalRef, info.columns)
    }

    class ResolvedBoard(
        val boardRef: EntityRef,
        val typeRef: EntityRef,
        val journalRef: EntityRef,
        val columns: List<BoardInfo.ColumnDef>
    )

    private fun loadCardsByStatus(
        board: ResolvedBoard,
        columns: List<BoardInfo.ColumnDef>,
        filter: Predicate?
    ): Map<String, List<CardRec>> {
        val (cardsSourceId, basePredicate) = resolveCardsSourceAndPredicate(board)
        // Per-column status clause; a column's `hideOldItems` is applied only to that column,
        // expressed as OR-of-(status AND hide) so all requested columns load in one query.
        val statusPredicate = Predicates.or(
            columns.map { col ->
                val hide = columnHidePredicate(col)
                if (hide != null) {
                    Predicates.and(Predicates.eq("_status", col.id), hide)
                } else {
                    Predicates.eq("_status", col.id)
                }
            }
        )
        val predicate = Predicates.and(
            basePredicate,
            statusPredicate,
            filter ?: VoidPredicate.INSTANCE
        )
        val cards = recordsService.query(
            RecordsQuery.create {
                withSourceId(cardsSourceId)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicate)
                withMaxItems(cardsHardCap)
            },
            CardRec::class.java
        ).getRecords()
        return cards.groupBy { it.status }
    }

    companion object {
        const val DEFAULT_MAX_ITEMS = 25

        /**
         * Predicate hiding cards whose status changed before the column's `hideItemsOlderThan` window,
         * or null when the column has no such window. The relative-date value (`-P30D`-style) is resolved
         * by the records/predicate engine — identical to what the UI used to send.
         */
        internal fun columnHidePredicate(col: BoardInfo.ColumnDef): Predicate? {
            if (!col.hideOldItems) return null
            val window = col.hideItemsOlderThan
            if (window.isNullOrBlank()) return null
            return ValuePredicate("_statusModified", ValuePredicate.Type.GE, "-$window")
        }
    }

    private fun loadColumnCards(board: ResolvedBoard, columnId: String): List<CardRec> {
        val (cardsSourceId, basePredicate) = resolveCardsSourceAndPredicate(board)
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(cardsSourceId)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(Predicates.and(basePredicate, Predicates.eq("_status", columnId)))
                withMaxItems(cardsHardCap)
            },
            CardRec::class.java
        ).getRecords()
    }

    internal fun resolveCardsSourceAndPredicate(board: ResolvedBoard): Pair<String, Predicate> {
        if (EntityRef.isNotEmpty(board.journalRef)) {
            val j = recordsService.getAtts(board.journalRef, JournalInfo::class.java)
            if (EntityRef.isNotEmpty(j.typeRef)) {
                return typeSourceId(j.typeRef) to j.predicate
            }
        }
        return typeSourceId(board.typeRef) to VoidPredicate.INSTANCE
    }

    private fun typeSourceId(typeRef: EntityRef): String {
        val src = ecosTypeService.getTypeInfo(typeRef)?.sourceId ?: ""
        require(src.isNotBlank()) { "Type $typeRef has no sourceId" }
        return src
    }

    /**
     * Effective rank keys for (board, grouping, column): the grouping's own ranks, falling back to
     * the flat ("") ranks for cards not yet ranked in this grouping. For the flat board, this is just
     * the flat ranks. Lets a freshly-grouped view inherit the flat manual order as its baseline.
     */
    private fun effectiveRankByCard(boardKey: String, workspace: String, grouping: String, columnId: String): Map<String, String> {
        val primary = orderRepo.findByBoardAndColumn(boardKey, workspace, grouping, columnId)
            .associate { it.cardRef to it.rankKey }
        if (grouping == BoardCardOrderDesc.GROUPING_FLAT) {
            return primary
        }
        val flat = orderRepo.findByBoardAndColumn(boardKey, workspace, BoardCardOrderDesc.GROUPING_FLAT, columnId)
            .associate { it.cardRef to it.rankKey }
        return flat + primary // grouping-specific rank overrides the flat fallback
    }

    /** unranked (by _created desc) ++ ranked (by rankKey asc). Stale order recs (columnId != _status) ignored. */
    private fun mergeColumn(boardKey: String, workspace: String, grouping: String, columnId: String, cards: List<CardRec>): List<CardRec> {
        if (cards.isEmpty()) return emptyList()
        val rankByCard = effectiveRankByCard(boardKey, workspace, grouping, columnId)
        val ranked = ArrayList<Pair<CardRec, String>>()
        val unranked = ArrayList<CardRec>()
        for (card in cards) {
            val rankKey = rankByCard[card.ref.toString()]
            if (rankKey != null) ranked.add(card to rankKey) else unranked.add(card)
        }
        unranked.sortByDescending { it.created }
        ranked.sortWith(compareBy({ it.second }, { it.first.ref.toString() }))
        return unranked + ranked.map { it.first }
    }
}
