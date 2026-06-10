package ru.citeck.ecos.uiserv.domain.board.dto

import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate

/**
 * Single source for a board column's *additional filter* — the predicate AND-ed into the column's
 * card query on top of its `_status` match, or null when the column adds none.
 *
 * It is exposed to the UI as the computed `additionalFilter` column attribute (see
 * `ResolvedBoardColumn`) and applied by the board-cards backend, so the per-column filtering rule
 * lives in exactly one place instead of being rebuilt independently on each side.
 *
 * Currently it is the `hideOldItems` recency cutoff: `_statusModified >= -<window>`. The relative-date
 * value (`-P30D`-style) is resolved by the records/predicate engine, identical to what the UI sent
 * before the rule moved here.
 */
object BoardColumnFilters {

    @JvmStatic
    fun additionalFilter(hideOldItems: Boolean, hideItemsOlderThan: String?): Predicate? {
        if (!hideOldItems || hideItemsOlderThan.isNullOrBlank()) {
            return null
        }
        return ValuePredicate("_statusModified", ValuePredicate.Type.GE, "-$hideItemsOlderThan")
    }
}
