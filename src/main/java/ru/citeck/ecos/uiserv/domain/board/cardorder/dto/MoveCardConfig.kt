package ru.citeck.ecos.uiserv.domain.board.cardorder.dto

import ru.citeck.ecos.webapp.api.entity.EntityRef

class MoveCardConfig(
    val board: EntityRef = EntityRef.EMPTY,
    val card: EntityRef = EntityRef.EMPTY,
    val column: String = "",
    /** insert right after this card in `column`; null/empty = top of column */
    val afterCard: EntityRef? = null,
    /** ordering context: "" = flat board, or the active swimlane grouping attribute id */
    val grouping: String = ""
)
