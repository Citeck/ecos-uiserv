package ru.citeck.ecos.uiserv.domain.board.cardorder.service

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.ctx.EcosContext
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
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
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.OrderRec
import ru.citeck.ecos.uiserv.domain.board.cardorder.repo.BoardCardOrderRepo
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnFilters
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
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
     * Curation-time source for [BoardCardOrderDesc.ATT_ORDERED_AT] stamps. Mutable for tests only:
     * the test fixture aligns it with its synthetic card-timestamp scale, so "statused after the last
     * move" scenarios stay deterministic. The anchor comparison is cross-clock by nature (uiserv
     * stamps vs the card source's `_statusModified`) — skew up to [ANCHOR_SKEW_MARGIN] is absorbed by
     * the read-side boundary shift (see [loadColumnContent]).
     */
    internal var clock: () -> Instant = { Instant.now() }

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

    /**
     * Per-column page request: which column, how many to skip, the page size, and the column's
     * [additionalFilter] — an extra predicate AND-ed into the column's card query (e.g. the
     * `hideOldItems` cutoff). The UI computes it from the column's `additionalFilter` attribute and
     * sends it here; when null, the column's own filter ([BoardColumnFilters.additionalFilter]) is
     * used as a fallback for callers that don't supply one.
     */
    class ColumnPageReq(
        val columnId: String = "",
        val skipCount: Int = 0,
        val maxItems: Int = DEFAULT_MAX_ITEMS,
        val additionalFilter: Predicate? = null
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
            board.columns.map {
                ColumnPageReq(it.id, 0, defaultMaxItems, BoardColumnFilters.additionalFilter(it.hideOldItems, it.hideItemsOlderThan))
            }
        } else {
            columnPages
        }
        require(reqs.any { colById.containsKey(it.columnId) }) { "No known columns requested for board $boardRef" }
        val (cardsSourceId, basePredicate) = resolveCardsSourceAndPredicate(board)
        // Each column's order is composed concurrently (see [loadColumnContents]); paging stays on this thread.
        val resByReq = loadColumnContents(
            reqs, colById, cardsSourceId, basePredicate, filter, ws, boardRefStr, grouping, board.cardOrderEnabled
        )
        return reqs.mapIndexedNotNull { idx, req ->
            val col = colById[req.columnId] ?: return@mapIndexedNotNull null
            val res = resByReq[idx]
            ColumnContent(
                columnId = col.id,
                name = col.name,
                // True column size from the query COUNTs (NOT the bounded fetch window), so the UI knows
                // there are more cards to page in. cards = the requested page out of the composed order.
                totalCount = res.totalCount,
                cards = res.refs.drop(req.skipCount.coerceAtLeast(0)).take(req.maxItems.coerceAtLeast(0))
            )
        }
    }

    /**
     * Composed order for every requested column, each built on its own virtual thread so total latency
     * tracks the slowest column rather than their sum. Per-column work: the order-row read, one batch
     * live-state read for rowed cards, and one or two card-source queries (see [loadColumnContent]). The
     * returned list is index-aligned with [reqs]; an unknown column maps to an empty result. The caller's
     * context (auth, workspace, locale) is captured via [EcosContext.getScopeData] and re-established
     * inside each worker so the queries run as the request would.
     */
    private fun loadColumnContents(
        reqs: List<ColumnPageReq>,
        colById: Map<String, BoardInfo.ColumnDef>,
        cardsSourceId: String,
        basePredicate: Predicate,
        filter: Predicate?,
        workspace: String,
        boardKey: String,
        grouping: String,
        cardOrderEnabled: Boolean
    ): List<OrderedColumn> {
        val scopeData = ecosContext.getScopeData()
        return Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = reqs.map { req ->
                val col = colById[req.columnId] ?: return@map null
                // Fetch only enough refs to satisfy this column's window: skipCount plus a page whose size is
                // the request's own maxItems, floored at [DEFAULT_MAX_ITEMS].
                val fetch = req.skipCount + maxOf(DEFAULT_MAX_ITEMS, req.maxItems)
                // The column's additional filter: the one the UI sent in the request, or — for callers that
                // don't supply one — the column's own (computed from the same [BoardColumnFilters] source).
                val additionalFilter = req.additionalFilter
                    ?: BoardColumnFilters.additionalFilter(col.hideOldItems, col.hideItemsOlderThan)
                executor.submit(
                    Callable {
                        ecosContext.newScope(scopeData).use {
                            loadColumnContent(
                                cardsSourceId, basePredicate, col, additionalFilter, filter,
                                workspace, boardKey, grouping, fetch, cardOrderEnabled
                            )
                        }
                    }
                )
            }
            futures.map { future ->
                if (future == null) {
                    OrderedColumn(emptyList(), 0)
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
     * [MoveCardAction.afterCard] (or above every ranked card when afterCard is null/empty).
     * Changing the column also changes the card's `_status`. [MoveCardAction.cards] may be just the
     * displayed prefix down to the insertion point +1 — but unranked cards left out of it sink below
     * the ranked block (their materialization is what pins them). Returns the assigned rank key.
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

        if (!board.cardOrderEnabled) {
            // manual ordering is off for this board: a move is just the status change above — no rank
            // rows are written or pruned, positioning (afterCard/cards) is ignored, cards follow the
            // plain query order
            return ""
        }

        // 2. column cards as the client renders them — the move's source of truth, NOT a re-fetch. The list
        // may be just the prefix down to the insertion point +1; it is already in display order and may
        // include the moved card (excluded where needed).
        val cards = cfg.cards
        val boardRefStr = boardKey(cfg.board)
        val grouping = cfg.grouping

        // Order rows + live states (status, statusModified), one batched read AFTER the status mutation so
        // the moved card's bumped `_statusModified` is observed — the rank row written below stores it as
        // the link key; a pre-mutation value would make the row born stale. Validity is checked as SYSTEM:
        // a mover who can't read some ranked card must not mistake its rows for stale ones.
        val colRows = loadColumnRows(
            boardRefStr,
            workspace,
            grouping,
            cfg.column,
            extraLiveRefs = cards.map { it.toString() } + cfg.card.toString(),
            readLiveAsSystem = true
        )
        val live = colRows.live

        // Self-heal: drop rows whose link key no longer matches — the card left the column, or left and came
        // back (the rank is from a previous "life"). Rows accumulate otherwise, because nothing deletes them
        // when a card leaves a column outside the move API (external `_status` changes, deletes).
        orderRepo.deleteRecords(colRows.invalid.map { it.recordRef })

        // This move is a curation act: every row it writes is stamped with the same [BoardCardOrderDesc.ATT_ORDERED_AT].
        val touch = clock()

        // Effective rank falls back to the flat ("") order for cards not yet ranked in this grouping,
        // so a freshly-grouped view inherits the flat manual order as its baseline. New keys are
        // written to this grouping, evolving it independently thereafter.
        val rankByCard = colRows.validByCard.entries.associateTo(HashMap()) { it.key to it.value.rankKey }

        // 3. fold unranked listed cards into rank keys at their displayed positions, stamping each row with
        // its card's live link key. Unranked cards form a prefix (the new block) and possibly a suffix (the
        // unranked tail below the ranked block); each key is computed between its in-list neighbours. Until
        // a ranked card is passed, the column's smallest existing key caps the fold, so a truncated list
        // can't interleave with the ranks of cards outside it.
        fun upsertRank(ref: EntityRef, key: String) {
            rankByCard[ref.toString()] = key
            orderRepo.upsert(
                boardRefStr,
                workspace,
                grouping,
                ref.toString(),
                cfg.column,
                key,
                live[ref.toString()]?.statusModified,
                touch
            )
        }
        if (rankByCard.isEmpty()) {
            val seeds = RankKeys.seedSpread(cards.size)
            cards.forEachIndexed { index, ref -> upsertRank(ref, seeds[index]) }
        } else if (cards.any { rankByCard[it.toString()] == null }) {
            val minRankedKey = rankByCard.values.min()
            var passedRanked = false
            var prevKey: String? = null
            for ((index, ref) in cards.withIndex()) {
                val existing = rankByCard[ref.toString()]
                if (existing != null) {
                    passedRanked = true
                    // DB keys are authoritative; on client/db order drift keep the walk monotonic
                    if (prevKey == null || existing > prevKey) prevKey = existing
                    continue
                }
                var nextKey: String? = null
                for (j in index + 1 until cards.size) {
                    val k = rankByCard[cards[j].toString()] ?: continue
                    if (prevKey == null || k > prevKey) {
                        nextKey = k
                    }
                    break
                }
                if (nextKey == null && !passedRanked && (prevKey == null || minRankedKey > prevKey)) {
                    nextKey = minRankedKey
                }
                val key = RankKeys.between(prevKey, nextKey)
                if (key.length > RankKeys.MAX_RANK_LEN) {
                    rebalanceForRetry(rebalanceDepth, boardRefStr, workspace, grouping, cfg.column)
                    return doMoveCard(cfg, workspace, rebalanceDepth + 1)
                }
                upsertRank(ref, key)
                prevKey = key
            }
        }

        // 4. fully-ranked display order, excluding the moved card
        val displayNow = cards.asSequence()
            .filter { it != cfg.card }
            .sortedWith(compareBy({ rankByCard.getValue(it.toString()) }, { it.toString() }))
            .toList()

        // 5. find insertion neighbours by `afterCard` (afterCard not in the list -> treat as top)
        val afterRef = cfg.afterCard?.takeIf { EntityRef.isNotEmpty(it) }
        val afterIdx = if (afterRef == null) -1 else displayNow.indexOfFirst { it == afterRef }
        val prevKey: String? = if (afterIdx >= 0) rankByCard.getValue(displayNow[afterIdx].toString()) else null
        val nextKey: String? = when {
            afterIdx >= 0 && afterIdx + 1 < displayNow.size -> rankByCard.getValue(displayNow[afterIdx + 1].toString())
            // drop to the very top: above every ranked card of the column, listed or not
            afterIdx < 0 -> rankByCard.filterKeys { it != cfg.card.toString() }.values.minOrNull()
            // drop after the last listed card: after everything known
            else -> null
        }

        val newKey = RankKeys.between(prevKey, nextKey)
        if (newKey.length > RankKeys.MAX_RANK_LEN) {
            rebalanceForRetry(rebalanceDepth, boardRefStr, workspace, grouping, cfg.column)
            return doMoveCard(cfg, workspace, rebalanceDepth + 1)
        }
        orderRepo.upsert(
            boardRefStr,
            workspace,
            grouping,
            cfg.card.toString(),
            cfg.column,
            newKey,
            live[cfg.card.toString()]?.statusModified,
            touch
        )
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
        orderRecs.forEachIndexed { index, rec ->
            // re-spreads keys only; each row keeps its link key and curation time
            orderRepo.upsert(
                boardRef,
                workspace,
                grouping,
                rec.cardRef,
                columnId,
                seeds[index],
                rec.cardStatusModified,
                rec.orderedAt
            )
        }
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
        return ResolvedBoard(boardRef, info.typeRef, info.journalRef, info.columns, !info.readOnly)
    }

    class ResolvedBoard(
        val boardRef: EntityRef,
        val typeRef: EntityRef,
        val journalRef: EntityRef,
        val columns: List<BoardInfo.ColumnDef>,
        /**
         * Manual card ordering rides the board's existing `canDrag` setting (stored as `readOnly = !canDrag`):
         * a no-drag board can never produce a move, so its load follows the plain query order and the rank
         * table is neither read nor written. The UI needs no own gate — `readOnly` already disables dragging.
         */
        val cardOrderEnabled: Boolean
    )

    /**
     * Query result for one column — refs only, NO per-card attributes — workspace-scoped, sorted by
     * [ATT_STATUS_MODIFIED] desc, with at most [maxItems] refs. Resolving no attributes keeps
     * card-loading effort proportional to the rendered page and avoids the per-record permission/attribute
     * fan-out over the whole column that caused multi-minute board loads. The column's
     * [additionalFilter] (e.g. the `hideOldItems` cutoff) is AND-ed into its own query only — so it constrains
     * both the page and the COUNT. [segmentFilter] is the `_statusModified` bound that splits the column
     * around the new-block anchor (see [loadColumnContent]); null = the whole column. The result's
     * [RecsQueryRes.getTotalCount] is the full segment COUNT (independent of [maxItems]) — used as the
     * authoritative size so the UI can page past the fetched window.
     */
    private fun queryColumnRefs(
        cardsSourceId: String,
        basePredicate: Predicate,
        col: BoardInfo.ColumnDef,
        additionalFilter: Predicate?,
        filter: Predicate?,
        segmentFilter: Predicate?,
        workspace: String,
        maxItems: Int
    ): RecsQueryRes<EntityRef> {
        val statusPredicate = if (additionalFilter != null) {
            Predicates.and(Predicates.eq("_status", col.id), additionalFilter)
        } else {
            Predicates.eq("_status", col.id)
        }
        val predicate = Predicates.and(
            basePredicate,
            statusPredicate,
            filter ?: VoidPredicate.INSTANCE,
            segmentFilter ?: VoidPredicate.INSTANCE
        )
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(cardsSourceId)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicate)
                withWorkspaces(listOf(workspace))
                withSortBy(SortBy(ATT_STATUS_MODIFIED, false))
                withMaxItems(maxItems)
            }
        )
    }

    companion object {
        const val DEFAULT_MAX_ITEMS = 25

        /** Page size for live-state getAtts batches (see [loadLiveStates]). */
        private const val LIVE_STATE_BATCH = 500

        /**
         * How far back from the anchor the new-block boundary sits (see [loadColumnContent]). Covers
         * uiserv-vs-card-source clock skew up to this value; beyond it (broken NTP) a card statused
         * right after a move can still sink into the tail.
         */
        private val ANCHOR_SKEW_MARGIN: Duration = Duration.ofMillis(500)

        /**
         * Cards are segmented and ordered by status-recency, not creation: a card whose `_status` just
         * changed (e.g. via a card action, which doesn't issue a move) surfaces in the "new" block on top,
         * consistent with the `hideOldItems` cutoff that also keys off `_statusModified`. The same attribute
         * is the rank rows' link key (see [BoardCardOrderDesc.ATT_CARD_STATUS_MODIFIED]).
         */
        private const val ATT_STATUS_MODIFIED = "_statusModified"
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

    private class ColumnRows(
        val validByCard: Map<String, OrderRec>,
        val invalid: List<OrderRec>,
        val live: Map<String, LiveCardState>
    )

    /**
     * Order rows of (board, grouping, column) with link-key validity already applied. A card's VALID
     * grouping row wins; when the grouping row is stale the card FALLS BACK to its valid flat ("") row —
     * a stale row must not shadow a usable fallback. For the flat board, just the flat rows. Lets a
     * freshly-grouped view inherit the flat manual order as its baseline.
     *
     * [invalid] collects every stale row seen (both groupings) for the move path to delete; the load path
     * ignores them (reads stay write-free). [extraLiveRefs] join the single live-state batch (the move
     * path needs the listed cards' states for link-key stamping). [readLiveAsSystem]: the move path checks
     * validity as system, so a mover who can't read some ranked card doesn't mistake its rows for stale
     * (and doesn't destroy other users' ordering); the load path reads as the user — rows of unreadable
     * cards stay invalid there, keeping unreadable refs out of the response.
     */
    private fun loadColumnRows(
        boardKey: String,
        workspace: String,
        grouping: String,
        columnId: String,
        extraLiveRefs: Collection<String> = emptyList(),
        readLiveAsSystem: Boolean = false
    ): ColumnRows {
        val primary = orderRepo.findByBoardAndColumn(boardKey, workspace, grouping, columnId)
        val flat = if (grouping == BoardCardOrderDesc.GROUPING_FLAT) {
            emptyList()
        } else {
            orderRepo.findByBoardAndColumn(boardKey, workspace, BoardCardOrderDesc.GROUPING_FLAT, columnId)
        }
        val liveRefs = LinkedHashSet(extraLiveRefs)
        primary.mapTo(liveRefs) { it.cardRef }
        flat.mapTo(liveRefs) { it.cardRef }
        val live = if (readLiveAsSystem) {
            AuthContext.runAsSystem { loadLiveStates(liveRefs) }
        } else {
            loadLiveStates(liveRefs)
        }
        val validByCard = HashMap<String, OrderRec>()
        val invalid = ArrayList<OrderRec>()
        for (row in flat) {
            if (isRowValid(row, live[row.cardRef], columnId)) validByCard[row.cardRef] = row else invalid.add(row)
        }
        for (row in primary) {
            if (isRowValid(row, live[row.cardRef], columnId)) validByCard[row.cardRef] = row else invalid.add(row)
        }
        return ColumnRows(validByCard, invalid, live)
    }

    private class LiveCardState(val status: String, val statusModified: Instant?)

    /**
     * Live `_status` / `_statusModified` for [cardRefs], batched by [LIVE_STATE_BATCH] (a deeply curated
     * column can hold thousands of rows — one giant getAtts would be a single oversized remote call).
     * Unreadable refs resolve to empty state.
     */
    private fun loadLiveStates(cardRefs: Collection<String>): Map<String, LiveCardState> {
        if (cardRefs.isEmpty()) return emptyMap()
        val result = HashMap<String, LiveCardState>(cardRefs.size)
        for (chunk in cardRefs.chunked(LIVE_STATE_BATCH)) {
            val atts = recordsService.getAtts(
                chunk.map { EntityRef.valueOf(it) },
                mapOf("status" to "_status?str", "statusModified" to "_statusModified")
            )
            chunk.forEachIndexed { i, ref ->
                result[ref] = LiveCardState(
                    atts[i].getAtt("status").asText(),
                    atts[i].getAtt("statusModified").getAs(Instant::class.java)
                )
            }
        }
        return result
    }

    /**
     * A rank row counts only while its card is still in the column AND the card's `_statusModified` still
     * equals the snapshot taken when the rank was written (the link key): any later status change — leaving
     * the column, or leaving and coming back — makes the row stale. A row WITHOUT a link key (not produced
     * by this code, e.g. a pre-link-key leftover) is stale too: ignored on read, reclaimed by the move-path
     * prune. There is deliberately NO legacy-row support — old deployments clean the order table instead.
     */
    private fun isRowValid(row: OrderRec, live: LiveCardState?, columnId: String): Boolean {
        if (live == null || live.status != columnId) return false
        val stored = row.cardStatusModified ?: return false
        return sameInstant(stored, live.statusModified)
    }

    /** Millisecond-precision equality: sub-milli precision may be lost in storage/serialization round-trips. */
    private fun sameInstant(a: Instant, b: Instant?): Boolean {
        return b != null && a.truncatedTo(ChronoUnit.MILLIS) == b.truncatedTo(ChronoUnit.MILLIS)
    }

    /** Old-model merge for columns with no link-key-stamped ranks: unranked (query order) ++ ranked (rankKey asc). */
    private fun mergeRefs(orderedRefs: List<EntityRef>, rankByCard: Map<String, String>): List<EntityRef> {
        if (orderedRefs.isEmpty() || rankByCard.isEmpty()) return orderedRefs
        val ranked = ArrayList<Pair<EntityRef, String>>()
        val unranked = ArrayList<EntityRef>()
        for (ref in orderedRefs) {
            val key = rankByCard[ref.toString()]
            if (key != null) ranked.add(ref to key) else unranked.add(ref)
        }
        ranked.sortWith(compareBy({ it.second }, { it.first.toString() }))
        return unranked + ranked.map { it.first }
    }

    private class OrderedColumn(val refs: List<EntityRef>, val totalCount: Long)

    /**
     * One column's display order and authoritative count. A curated column loads as THREE segments
     * partitioned by `anchor = max(orderedAt over valid ranks)` — when the ordering was last curated:
     *
     *   [`_statusModified` > anchor: the "new" block, ts desc — cards whose status changed after the
     *    last curation act; always on top]
     *   ++ [valid ranked rows, rankKey asc — read from the order table, NOT window-dependent]
     *   ++ [`_statusModified` <= anchor and never ranked: the tail, ts desc — cards the curator saw
     *    (or could have seen) and left unranked; below the curated block]
     *
     * totalCount = COUNT(> anchor) + COUNT(<= anchor) = the full filtered column count. Columns with no
     * valid rows keep the single-window behavior (plain query order).
     *
     * With [additionalFilter]/[filter] active a ranked card must also match them; membership in the
     * query windows is the confirmation. A matching ranked card missed by those windows in a huge column
     * degrades to "shown when its page is reached" — the same accepted limitation the old single-window
     * model had for ALL ranked cards, now confined to the filtered case.
     */
    private fun loadColumnContent(
        cardsSourceId: String,
        basePredicate: Predicate,
        col: BoardInfo.ColumnDef,
        additionalFilter: Predicate?,
        filter: Predicate?,
        workspace: String,
        boardKey: String,
        grouping: String,
        fetch: Int,
        cardOrderEnabled: Boolean
    ): OrderedColumn {
        if (!cardOrderEnabled) {
            // manual ordering is off for this board: plain query order, the rank table is not consulted
            // (no order-row reads, no live-state batches) — the pre-ordering behavior
            val res = queryColumnRefs(cardsSourceId, basePredicate, col, additionalFilter, filter, null, workspace, fetch)
            return OrderedColumn(res.getRecords(), res.getTotalCount())
        }
        val validRows = loadColumnRows(boardKey, workspace, grouping, col.id).validByCard.values
        // The anchor = when the column's ordering was last CURATED (max orderedAt over valid rows) —
        // deliberately NOT the cards' own `_statusModified` snapshots: those lag behind reality (a
        // within-column move doesn't bump them at all), so a never-ranked card with a merely newer
        // status change would float above a freshly arranged column forever.
        val anchor = validRows.mapNotNull { it.orderedAt }.maxOrNull()
        if (anchor == null) {
            // no valid ranks -> an uncurated column: plain single-window query order
            val res = queryColumnRefs(cardsSourceId, basePredicate, col, additionalFilter, filter, null, workspace, fetch)
            return OrderedColumn(mergeRefs(res.getRecords(), validRows.associate { it.cardRef to it.rankKey }), res.getTotalCount())
        }
        // The boundary is the anchor shifted back by the skew margin: the anchor is on uiserv's clock,
        // `_statusModified` on the card source's — if the card source's clock lagged, a card statused
        // right after a move would compute as "before" it and silently sink into the tail. The margin
        // absorbs NTP-level skew; the price is benign: cards statused within the margin BEFORE the move
        // float above the fresh order — same as if the move had happened half a second later — and the
        // next move folds them in.
        val boundary = anchor.minus(ANCHOR_SKEW_MARGIN)
        val newRes = queryColumnRefs(
            cardsSourceId,
            basePredicate,
            col,
            additionalFilter,
            filter,
            ValuePredicate(ATT_STATUS_MODIFIED, ValuePredicate.Type.GT, boundary),
            workspace,
            fetch
        )
        val restRes = queryColumnRefs(
            cardsSourceId,
            basePredicate,
            col,
            additionalFilter,
            filter,
            ValuePredicate(ATT_STATUS_MODIFIED, ValuePredicate.Type.LE, boundary),
            workspace,
            fetch + validRows.size
        )
        val rankedSorted = validRows.sortedWith(compareBy({ it.rankKey }, { it.cardRef }))
        val rankedRefs = rankedSorted.mapTo(HashSet()) { it.cardRef }
        val restWindow = restRes.getRecords()
        val filtersActive = additionalFilter != null || filter != null
        val ranked = if (filtersActive) {
            // confirm against BOTH windows: a valid ranked card can land in the new-block window instead
            // of the tail window (its `_statusModified` is in the card source's clock domain, the anchor
            // in uiserv's — skew can put it above) — it must still count as filter-matching there
            val matching = restWindow.mapTo(HashSet()) { it.toString() }
            newRes.getRecords().mapTo(matching) { it.toString() }
            rankedSorted.filter { it.cardRef in matching }.map { EntityRef.valueOf(it.cardRef) }
        } else {
            rankedSorted.map { EntityRef.valueOf(it.cardRef) }
        }
        // ranked refs can surface in the query windows (clock skew puts them above the boundary) — dedupe
        val newBlock = newRes.getRecords().filter { it.toString() !in rankedRefs }
        val tail = restWindow.filter { it.toString() !in rankedRefs }
        return OrderedColumn(newBlock + ranked + tail, newRes.getTotalCount() + restRes.getTotalCount())
    }
}
