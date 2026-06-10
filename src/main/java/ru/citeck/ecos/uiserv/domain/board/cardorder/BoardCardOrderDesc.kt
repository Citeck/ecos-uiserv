package ru.citeck.ecos.uiserv.domain.board.cardorder

object BoardCardOrderDesc {

    const val SOURCE_ID = "board-card-order"
    const val TYPE_ID = "board-card-order"
    const val DB_TABLE = "ecos_board_card_order"

    const val ATT_BOARD_REF = "boardRef"
    const val ATT_CARD_REF = "cardRef"
    const val ATT_COLUMN_ID = "columnId"
    const val ATT_RANK_KEY = "rankKey"

    /**
     * Snapshot of the card's `_statusModified` taken when the rank was written — the "link key" between
     * the card and its order row. `_statusModified` changes only on a real status change, so a mismatch
     * means the card left the column (or left and came back) after ranking: the rank is stale.
     * A row without it is treated as stale (no legacy-row support).
     */
    const val ATT_CARD_STATUS_MODIFIED = "cardStatusModified"

    /**
     * When the column's ordering was last curated — every row touched by a move gets the move's
     * timestamp. The "new block" anchor is `max(orderedAt over the column's valid rows)`: only cards
     * whose `_statusModified` is newer than the last curation act float above the ranked block.
     * (Deliberately NOT the card's `_statusModified`: that lags behind reality — see
     * [ATT_CARD_STATUS_MODIFIED], whose only job is staleness detection.)
     */
    const val ATT_ORDERED_AT = "orderedAt"

    /** Grouping context the order belongs to: "" = flat board, or the swimlane grouping attribute id. */
    const val ATT_GROUPING = "grouping"
    const val GROUPING_FLAT = ""
}
