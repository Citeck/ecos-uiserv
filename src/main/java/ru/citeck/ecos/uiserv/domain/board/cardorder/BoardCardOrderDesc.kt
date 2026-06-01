package ru.citeck.ecos.uiserv.domain.board.cardorder

object BoardCardOrderDesc {

    const val SOURCE_ID = "board-card-order"
    const val TYPE_ID = "board-card-order"
    const val DB_TABLE = "ecos_board_card_order"

    const val ATT_BOARD_REF = "boardRef"
    const val ATT_CARD_REF = "cardRef"
    const val ATT_COLUMN_ID = "columnId"
    const val ATT_RANK_KEY = "rankKey"

    /** Grouping context the order belongs to: "" = flat board, or the swimlane grouping attribute id. */
    const val ATT_GROUPING = "grouping"
    const val GROUPING_FLAT = ""
}
