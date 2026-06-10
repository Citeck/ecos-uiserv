package ru.citeck.ecos.uiserv.domain.board.cardorder.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ColumnContent(
    val columnId: String,
    val name: MLText,
    val totalCount: Long,
    /** card refs for the requested page, already in display order */
    val cards: List<EntityRef>
)
