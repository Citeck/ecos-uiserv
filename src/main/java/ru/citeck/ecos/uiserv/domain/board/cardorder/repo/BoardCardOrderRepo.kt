package ru.citeck.ecos.uiserv.domain.board.cardorder.repo

import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.board.cardorder.BoardCardOrderDesc
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.OrderRec
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BoardCardOrderRepo(private val recordsService: RecordsService) {

    private val maxItems = 5000

    fun findByBoard(boardRef: String): List<OrderRec> = query(
        Predicates.eq(BoardCardOrderDesc.ATT_BOARD_REF, boardRef)
    )

    fun findByBoardAndColumn(boardRef: String, grouping: String, columnId: String): List<OrderRec> = query(
        Predicates.and(
            Predicates.eq(BoardCardOrderDesc.ATT_BOARD_REF, boardRef),
            Predicates.eq(BoardCardOrderDesc.ATT_GROUPING, grouping),
            Predicates.eq(BoardCardOrderDesc.ATT_COLUMN_ID, columnId)
        )
    )

    fun findByBoardAndCard(boardRef: String, grouping: String, cardRef: String): OrderRec? = query(
        Predicates.and(
            Predicates.eq(BoardCardOrderDesc.ATT_BOARD_REF, boardRef),
            Predicates.eq(BoardCardOrderDesc.ATT_GROUPING, grouping),
            Predicates.eq(BoardCardOrderDesc.ATT_CARD_REF, cardRef)
        )
    ).firstOrNull()

    fun upsert(boardRef: String, grouping: String, cardRef: String, columnId: String, rankKey: String) {
        val existing = findByBoardAndCard(boardRef, grouping, cardRef)
        val atts = mapOf(
            BoardCardOrderDesc.ATT_BOARD_REF to boardRef,
            BoardCardOrderDesc.ATT_GROUPING to grouping,
            BoardCardOrderDesc.ATT_CARD_REF to cardRef,
            BoardCardOrderDesc.ATT_COLUMN_ID to columnId,
            BoardCardOrderDesc.ATT_RANK_KEY to rankKey
        )
        if (existing == null) {
            recordsService.create(BoardCardOrderDesc.SOURCE_ID, atts)
        } else {
            recordsService.mutate(existing.recordRef, atts)
        }
    }

    fun deleteByBoard(boardRef: String) {
        val refs = findByBoard(boardRef).map { it.recordRef }
        if (refs.isNotEmpty()) {
            recordsService.delete(refs)
        }
    }

    fun deleteRecords(refs: Collection<EntityRef>) {
        if (refs.isNotEmpty()) recordsService.delete(refs.toList())
    }

    private fun query(predicate: Predicate): List<OrderRec> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(BoardCardOrderDesc.SOURCE_ID)
                withQuery(predicate)
                withMaxItems(maxItems)
            },
            OrderRec::class.java
        ).getRecords()
    }
}
