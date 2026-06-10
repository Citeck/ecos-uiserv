package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnFilters
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColumnHidePredicateTest {

    @Test
    fun `no hide config returns null`() {
        assertNull(BoardColumnFilters.additionalFilter(hideOldItems = false, hideItemsOlderThan = null))
        assertNull(BoardColumnFilters.additionalFilter(hideOldItems = false, hideItemsOlderThan = "P30D"))
        assertNull(BoardColumnFilters.additionalFilter(hideOldItems = true, hideItemsOlderThan = null))
        assertNull(BoardColumnFilters.additionalFilter(hideOldItems = true, hideItemsOlderThan = ""))
    }

    @Test
    fun `hide config builds a relative-date ge predicate on _statusModified`() {
        val p = BoardColumnFilters.additionalFilter(hideOldItems = true, hideItemsOlderThan = "P30D")
        assertTrue(p is ValuePredicate)
        p as ValuePredicate
        assertEquals("_statusModified", p.getAttribute())
        assertEquals(ValuePredicate.Type.GE, p.getType())
        assertEquals("-P30D", p.getValue().asText())
    }
}
