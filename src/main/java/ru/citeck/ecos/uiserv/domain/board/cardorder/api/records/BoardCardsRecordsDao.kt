package ru.citeck.ecos.uiserv.domain.board.cardorder.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Unified load API for board cards. A single query selects which columns to load and how to page
 * each one via the `columns` argument — load one column, several, or all at once.
 *
 * Query body:
 * ```
 * {
 *   board: <boardRef>,
 *   columns: [ { id, skipCount?, maxItems?, additionalFilter? }, ... ]?,  // omit/empty -> all board columns
 *   maxItemsPerColumn?: <int>,                          // default page size when a column omits maxItems
 *   filter?: <predicate>                                // AND-ed into every column (search/journal/swimlane row)
 * }
 * `additionalFilter` is the column's own extra predicate (e.g. the `hideOldItems` cutoff); it constrains
 * both that column's page and its `totalCount`.
 * ```
 * Returns one record per requested column: `{ columnId, totalCount, cards[] }`, cards in persisted order:
 * the "new" block (cards whose `_statusModified` is newer than every rank's link key, ts desc), then the
 * manually ranked block (rankKey asc), then the unranked tail (older never-ranked cards, ts desc).
 */
@Component
class BoardCardsRecordsDao(
    private val service: BoardCardOrderService
) : AbstractRecordsDao(),
    RecordsQueryDao {

    companion object {
        const val ID = "board-cards"
        const val DEFAULT_PAGE = BoardCardOrderService.DEFAULT_MAX_ITEMS
    }

    override fun getId() = ID

    override fun queryRecords(recordsQuery: RecordsQuery): Any {
        val query = recordsQuery.getQuery(Query::class.java)
        val defaultMaxItems = query.maxItemsPerColumn?.takeIf { it > 0 } ?: DEFAULT_PAGE
        val columnPages = query.columns?.takeIf { it.isNotEmpty() }?.map { cfg ->
            BoardCardOrderService.ColumnPageReq(
                columnId = cfg.id,
                skipCount = cfg.skipCount ?: 0,
                maxItems = cfg.maxItems?.takeIf { it > 0 } ?: defaultMaxItems,
                additionalFilter = cfg.additionalFilter
            )
        }
        // Order is workspaceScope=PRIVATE: scope the read to the workspace the UI is viewing.
        val workspace = recordsQuery.workspaces.firstOrNull() ?: ""
        val columns = service.getBoardCards(
            query.board,
            columnPages,
            query.filter,
            query.grouping,
            defaultMaxItems,
            workspace
        )
        val res = RecsQueryRes<ColumnRecord>()
        res.setRecords(columns.map { ColumnRecord(query.board, it.columnId, it.name, it.totalCount, it.cards) })
        res.setTotalCount(columns.size.toLong())
        return res
    }

    class Query {
        var board: EntityRef = EntityRef.EMPTY
        var columns: List<ColumnPageCfg>? = null
        var maxItemsPerColumn: Int? = null
        var filter: Predicate? = null

        /** Ordering context: "" = flat board, or the active swimlane grouping attribute id. */
        var grouping: String = ""
    }

    class ColumnPageCfg {
        var id: String = ""
        var skipCount: Int? = null
        var maxItems: Int? = null

        /** Extra predicate AND-ed into this column's card query (the UI sends the column's `additionalFilter`). */
        var additionalFilter: Predicate? = null
    }

    /** Synthetic record representing one board column with the requested page of its cards. */
    class ColumnRecord(
        private val boardRef: EntityRef,
        val columnId: String,
        @AttName("name") val name: MLText,
        val totalCount: Long,
        val cards: List<EntityRef>
    ) {
        fun getId(): String = boardRef.getLocalId() + "$" + columnId

        @AttName("?disp")
        fun getDisplayName(): MLText = name
    }
}
