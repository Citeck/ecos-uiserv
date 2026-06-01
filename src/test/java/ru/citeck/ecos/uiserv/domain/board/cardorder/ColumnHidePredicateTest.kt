package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.BoardInfo
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColumnHidePredicateTest {

    @Test
    fun `no hide config returns null`() {
        assertNull(BoardCardOrderService.columnHidePredicate(BoardInfo.ColumnDef(id = "col1")))
        assertNull(
            BoardCardOrderService.columnHidePredicate(
                BoardInfo.ColumnDef(id = "col1", hideOldItems = false, hideItemsOlderThan = "P30D")
            )
        )
        assertNull(
            BoardCardOrderService.columnHidePredicate(
                BoardInfo.ColumnDef(id = "col1", hideOldItems = true, hideItemsOlderThan = null)
            )
        )
        assertNull(
            BoardCardOrderService.columnHidePredicate(
                BoardInfo.ColumnDef(id = "col1", hideOldItems = true, hideItemsOlderThan = "")
            )
        )
    }

    @Test
    fun `hide config builds a relative-date ge predicate on _statusModified`() {
        val p = BoardCardOrderService.columnHidePredicate(
            BoardInfo.ColumnDef(id = "col1", hideOldItems = true, hideItemsOlderThan = "P30D")
        )
        assertTrue(p is ValuePredicate)
        p as ValuePredicate
        assertEquals("_statusModified", p.getAttribute())
        assertEquals(ValuePredicate.Type.GE, p.getType())
        assertEquals("-P30D", p.getValue().asText())
    }
}
