package ru.citeck.ecos.uiserv.domain.board.cardorder.dto

import ru.citeck.ecos.webapp.api.entity.EntityRef

/** Payload of the `boards-service` `move-card` action. */
class MoveCardAction(
    val board: EntityRef = EntityRef.EMPTY,
    val card: EntityRef = EntityRef.EMPTY,
    val column: String = "",
    /** insert right after this card in `column`; null/empty = top of column */
    val afterCard: EntityRef? = null,
    /** ordering context: "" = flat board, or the active swimlane grouping attribute id */
    val grouping: String = "",
    /** viewing workspace the order is scoped to (independent of the board's own workspace); "" = default */
    val workspace: String = ""
)
