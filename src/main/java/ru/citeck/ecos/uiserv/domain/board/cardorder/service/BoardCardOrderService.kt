package ru.citeck.ecos.uiserv.domain.board.cardorder.service

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.ctx.EcosContext
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.BoardCardOrderDesc
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.BoardInfo
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.ColumnContent
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.JournalInfo
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.MoveCardAction
import ru.citeck.ecos.uiserv.domain.board.cardorder.repo.BoardCardOrderRepo
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

@Service
class BoardCardOrderService(
    private val recordsService: RecordsService,
    private val orderRepo: BoardCardOrderRepo,
    private val ecosTypeService: EcosTypeService,
    private val boardService: BoardService,
    private val workspaceService: WorkspaceService,
    private val ecosContext: EcosContext
) {

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
     * Each returned [ColumnContent] carries [ColumnContent.totalCount] (the authoritative column count from
     * the query, independent of the fetched window) and the requested page of card refs in persisted display
     * order. [filter] is AND-ed into every column's card query (e.g. text search, journal filters, a swimlane-row predicate).
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
        require(reqs.any { colById.containsKey(it.columnId) }) { "No known columns requested for board $boardRef" }
        val (cardsSourceId, basePredicate) = resolveCardsSourceAndPredicate(board)
        // Refs for each column are loaded concurrently (see [loadColumnRefs]); ordering stays on this thread.
        val resByReq = loadColumnRefs(reqs, colById, cardsSourceId, basePredicate, filter, ws)
        return reqs.mapIndexedNotNull { idx, req ->
            val col = colById[req.columnId] ?: return@mapIndexedNotNull null
            val res = resByReq[idx]
            val ordered = mergeColumn(boardRefStr, ws, grouping, col.id, res.getRecords())
            ColumnContent(
                columnId = col.id,
                name = col.name,
                // True column size from the query's COUNT (NOT the bounded fetch window), so the UI knows
                // there are more cards to page in. cards = the requested page out of the ordered fetch window.
                totalCount = res.getTotalCount(),
                cards = ordered.drop(req.skipCount.coerceAtLeast(0)).take(req.maxItems.coerceAtLeast(0))
            )
        }
    }

    /**
     * Refs for every requested column, each loaded on its own virtual thread (one query per column) so total
     * latency tracks the slowest column rather than their sum. The returned list is index-aligned with [reqs];
     * an unknown column maps to an empty list. The caller's context (auth, workspace, locale) is captured via
     * [EcosContext.getScopeData] and re-established inside each worker so the queries run as the request would.
     */
    private fun loadColumnRefs(
        reqs: List<ColumnPageReq>,
        colById: Map<String, BoardInfo.ColumnDef>,
        cardsSourceId: String,
        basePredicate: Predicate,
        filter: Predicate?,
        workspace: String
    ): List<RecsQueryRes<EntityRef>> {
        val scopeData = ecosContext.getScopeData()
        return Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = reqs.map { req ->
                val col = colById[req.columnId] ?: return@map null
                // Fetch only enough refs to satisfy this column's window: skipCount plus a page whose size is
                // the request's own maxItems, floored at [DEFAULT_MAX_ITEMS].
                val fetch = req.skipCount + maxOf(DEFAULT_MAX_ITEMS, req.maxItems)
                executor.submit(
                    Callable {
                        ecosContext.newScope(scopeData).use {
                            queryColumnRefs(cardsSourceId, basePredicate, col, filter, workspace, fetch)
                        }
                    }
                )
            }
            futures.map { future ->
                if (future == null) {
                    RecsQueryRes<EntityRef>()
                } else {
                    try {
                        future.get()
                    } catch (e: ExecutionException) {
                        throw e.cause ?: e
                    }
                }
            }
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

        // 2. column cards as the client renders them — the move's source of truth, NOT a re-fetch. The list
        // is already in display order, so the unranked prefix is in `_created` desc order without re-sorting.
        // May include the moved card; it is excluded where needed.
        val cards = cfg.cards
        val boardRefStr = boardKey(cfg.board)
        val grouping = cfg.grouping
        // Self-heal: order rows aren't removed when a card leaves a column (external `_status` changes, deletes),
        // so they accumulate. Drop this column's rows whose card is no longer in it before we read/extend them.
        pruneStaleOrderRows(boardRefStr, workspace, grouping, cfg.column, cards)
        // Effective rank falls back to the flat ("") order for cards not yet ranked in this grouping,
        // so a freshly-grouped view inherits the flat manual order as its baseline. New keys are
        // written to this grouping, evolving it independently thereafter.
        val rankByCard = effectiveRankByCard(boardRefStr, workspace, grouping, cfg.column).toMutableMap()

        // 3. fold the unranked prefix into rank keys below the smallest existing key
        val unranked = cards.filter { rankByCard[it.toString()] == null }
        if (unranked.isNotEmpty()) {
            val rankedSorted = cards.filter { rankByCard[it.toString()] != null }
                .sortedWith(compareBy({ rankByCard.getValue(it.toString()) }, { it.toString() }))
            if (rankedSorted.isEmpty()) {
                val seeds = RankKeys.seedSpread(unranked.size)
                unranked.forEachIndexed { index, ref ->
                    rankByCard[ref.toString()] = seeds[index]
                    orderRepo.upsert(boardRefStr, workspace, grouping, ref.toString(), cfg.column, seeds[index])
                }
            } else {
                val firstRankedKey = rankByCard.getValue(rankedSorted.first().toString())
                var lowerKey: String? = null
                for (ref in unranked) {
                    val key = RankKeys.between(lowerKey, firstRankedKey)
                    if (key.length > RankKeys.MAX_RANK_LEN) {
                        rebalanceForRetry(rebalanceDepth, boardRefStr, workspace, grouping, cfg.column)
                        return doMoveCard(cfg, workspace, rebalanceDepth + 1)
                    }
                    rankByCard[ref.toString()] = key
                    orderRepo.upsert(boardRefStr, workspace, grouping, ref.toString(), cfg.column, key)
                    lowerKey = key
                }
            }
        }

        // 4. fully-ranked display order, excluding the moved card
        val displayNow = cards.asSequence()
            .filter { it != cfg.card }
            .sortedWith(compareBy({ rankByCard.getValue(it.toString()) }, { it.toString() }))
            .toList()

        // 5. find insertion neighbours by `afterCard` (afterCard not in column -> treat as top)
        val afterRef = cfg.afterCard?.takeIf { EntityRef.isNotEmpty(it) }
        val afterIdx = if (afterRef == null) -1 else displayNow.indexOfFirst { it == afterRef }
        val prevKey: String? = if (afterIdx >= 0) rankByCard.getValue(displayNow[afterIdx].toString()) else null
        val nextKey: String? = when {
            afterIdx >= 0 && afterIdx + 1 < displayNow.size -> rankByCard.getValue(displayNow[afterIdx + 1].toString())
            afterIdx < 0 && displayNow.isNotEmpty() -> rankByCard.getValue(displayNow[0].toString())
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

    /**
     * Query result for one column — refs only, NO per-card attributes — workspace-scoped, sorted by
     * [RecordConstants.ATT_CREATED] desc, with at most [maxItems] refs. Resolving no attributes keeps
     * card-loading effort proportional to the rendered page and avoids the per-record permission/attribute
     * fan-out over the whole column that caused multi-minute board loads. The unranked display order IS this
     * created-desc order; ranked cards are re-ordered from the order table in [mergeColumn]. A column's
     * `hideOldItems` window is AND-ed into its own query only. The result's [RecsQueryRes.getTotalCount] is
     * the full column COUNT (independent of [maxItems]) — used as the authoritative column size so the UI can
     * page past the fetched window.
     */
    private fun queryColumnRefs(
        cardsSourceId: String,
        basePredicate: Predicate,
        col: BoardInfo.ColumnDef,
        filter: Predicate?,
        workspace: String,
        maxItems: Int
    ): RecsQueryRes<EntityRef> {
        val hide = columnHidePredicate(col)
        val statusPredicate = if (hide != null) {
            Predicates.and(Predicates.eq("_status", col.id), hide)
        } else {
            Predicates.eq("_status", col.id)
        }
        val predicate = Predicates.and(basePredicate, statusPredicate, filter ?: VoidPredicate.INSTANCE)
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(cardsSourceId)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicate)
                withWorkspaces(listOf(workspace))
                withSortBy(SortBy(RecordConstants.ATT_CREATED, false))
                withMaxItems(maxItems)
            }
        )
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

    /**
     * Delete this column's order rows whose card is no longer in it (live `_status` != [columnId]). Order rows
     * aren't removed when a card leaves a column (external `_status` changes, deletes), so they accumulate. Cards
     * present in [currentCards] are known to be in the column and skipped; the rest are status-checked in one
     * batch. Runs only on the (low-frequency) move path, so accumulated rows are reclaimed lazily and reads stay
     * bounded without a write-on-read.
     */
    private fun pruneStaleOrderRows(
        boardKey: String,
        workspace: String,
        grouping: String,
        columnId: String,
        currentCards: List<EntityRef>
    ) {
        val present = currentCards.mapTo(HashSet()) { it.toString() }
        val candidates = orderRepo.findByBoardAndColumn(boardKey, workspace, grouping, columnId)
            .filter { it.cardRef !in present }
        if (candidates.isEmpty()) return
        val statuses = recordsService.getAtts(candidates.map { EntityRef.valueOf(it.cardRef) }, mapOf("status" to "_status?str"))
        val stale = candidates.filterIndexed { i, _ -> statuses[i].getAtt("status").asText() != columnId }.map { it.recordRef }
        orderRepo.deleteRecords(stale)
    }

    /**
     * unranked (kept in the incoming `_created` desc query order) ++ ranked (by rankKey asc). A ranked card
     * whose old `_created` falls outside the created-desc fetch window is not shown until its page is reached
     * — an accepted limitation for large columns. Stale order rows (the card has left the column) never reach
     * here because they aren't in [orderedRefs], and are pruned on the move path (see [pruneStaleOrderRows]).
     */
    private fun mergeColumn(
        boardKey: String,
        workspace: String,
        grouping: String,
        columnId: String,
        orderedRefs: List<EntityRef>
    ): List<EntityRef> {
        if (orderedRefs.isEmpty()) return emptyList()
        val rankByCard = effectiveRankByCard(boardKey, workspace, grouping, columnId)
        val ranked = ArrayList<Pair<EntityRef, String>>()
        val unranked = ArrayList<EntityRef>()
        for (ref in orderedRefs) {
            val rankKey = rankByCard[ref.toString()]
            if (rankKey != null) ranked.add(ref to rankKey) else unranked.add(ref)
        }
        ranked.sortWith(compareBy({ it.second }, { it.first.toString() }))
        return unranked + ranked.map { it.first }
    }
}
