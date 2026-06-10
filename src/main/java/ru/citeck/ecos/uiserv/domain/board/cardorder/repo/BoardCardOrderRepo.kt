package ru.citeck.ecos.uiserv.domain.board.cardorder.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.board.cardorder.BoardCardOrderDesc
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.OrderRec
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

/**
 * Order records live in a `workspaceScope: PRIVATE` type. The per-view reads filter by the order
 * [workspace] (`withWorkspaces`) and create stamps it (`_workspace`); the board-cleanup read/delete is
 * workspace-agnostic (a board's order may exist in several viewing workspaces). The order workspace is
 * independent of the board's own workspace, which is encoded in [boardRef].
 */
@Component
class BoardCardOrderRepo(private val recordsService: RecordsService) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    /** Per-query row cap. Mutable for tests only (exercising truncation/loop behavior without 5000 rows). */
    internal var maxItems = 5000

    /** Workspace-agnostic: all order rows of a board across every (viewing) workspace. For cleanup. */
    fun findByBoard(boardRef: String): List<OrderRec> = query(
        null,
        Predicates.eq(BoardCardOrderDesc.ATT_BOARD_REF, boardRef)
    )

    fun findByBoardAndColumn(boardRef: String, workspace: String, grouping: String, columnId: String): List<OrderRec> {
        val records = query(
            workspace,
            Predicates.and(
                Predicates.eq(BoardCardOrderDesc.ATT_BOARD_REF, boardRef),
                Predicates.eq(BoardCardOrderDesc.ATT_GROUPING, grouping),
                Predicates.eq(BoardCardOrderDesc.ATT_COLUMN_ID, columnId)
            )
        )
        if (records.size >= maxItems) {
            // The rankKey-asc sort makes the truncation deterministic: the TOP of the curated order
            // survives, the deepest ranks degrade to unranked — consistently between loads and moves.
            log.warn {
                "board-card-order column read hit the $maxItems cap and was truncated " +
                    "(board=$boardRef, workspace='$workspace', grouping='$grouping', column=$columnId)"
            }
        }
        return records
    }

    fun findByBoardAndCard(boardRef: String, workspace: String, grouping: String, cardRef: String): OrderRec? = query(
        workspace,
        Predicates.and(
            Predicates.eq(BoardCardOrderDesc.ATT_BOARD_REF, boardRef),
            Predicates.eq(BoardCardOrderDesc.ATT_GROUPING, grouping),
            Predicates.eq(BoardCardOrderDesc.ATT_CARD_REF, cardRef)
        )
    ).firstOrNull()

    fun upsert(
        boardRef: String,
        workspace: String,
        grouping: String,
        cardRef: String,
        columnId: String,
        rankKey: String,
        cardStatusModified: Instant? = null
    ) {
        val existing = findByBoardAndCard(boardRef, workspace, grouping, cardRef)
        val atts = mutableMapOf<String, Any?>(
            BoardCardOrderDesc.ATT_BOARD_REF to boardRef,
            BoardCardOrderDesc.ATT_GROUPING to grouping,
            BoardCardOrderDesc.ATT_CARD_REF to cardRef,
            BoardCardOrderDesc.ATT_COLUMN_ID to columnId,
            BoardCardOrderDesc.ATT_RANK_KEY to rankKey,
            BoardCardOrderDesc.ATT_CARD_STATUS_MODIFIED to cardStatusModified
        )
        if (existing == null) {
            // Workspace is immutable for a record, so it is set only on create.
            atts[RecordConstants.ATT_WORKSPACE] = workspace
            recordsService.create(BoardCardOrderDesc.SOURCE_ID, atts)
        } else {
            recordsService.mutate(existing.recordRef, atts)
        }
    }

    /**
     * Workspace-agnostic: removes a board's order rows across every (viewing) workspace. Loops because a
     * single read is capped at [maxItems], and the board-wide row count (all workspaces × groupings ×
     * columns) can exceed it — leftovers would be orphaned forever (the board is gone, no move will ever
     * prune them).
     */
    fun deleteByBoard(boardRef: String) {
        while (true) {
            val refs = findByBoard(boardRef).map { it.recordRef }
            if (refs.isEmpty()) {
                return
            }
            recordsService.delete(refs)
        }
    }

    fun deleteRecords(refs: Collection<EntityRef>) {
        if (refs.isNotEmpty()) recordsService.delete(refs.toList())
    }

    /** [workspace] = null scopes to all workspaces available to the caller (system => every workspace). */
    private fun query(workspace: String?, predicate: Predicate): List<OrderRec> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(BoardCardOrderDesc.SOURCE_ID)
                withQuery(predicate)
                if (workspace != null) {
                    withWorkspaces(listOf(workspace))
                }
                // deterministic result (and deterministic truncation at the cap): curated-order top first
                withSortBy(SortBy(BoardCardOrderDesc.ATT_RANK_KEY, true))
                withMaxItems(maxItems)
            },
            OrderRec::class.java
        ).getRecords()
    }
}
