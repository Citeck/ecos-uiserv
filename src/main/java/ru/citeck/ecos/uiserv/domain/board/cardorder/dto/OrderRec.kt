package ru.citeck.ecos.uiserv.domain.board.cardorder.dto

import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

class OrderRec(
    @AttName("?id") val recordRef: EntityRef = EntityRef.EMPTY,
    // boardRef/cardRef are ENTITY_REF attributes; read their ref id as a string for keying
    @AttName("boardRef?id") val boardRef: String = "",
    @AttName("cardRef?id") val cardRef: String = "",
    val columnId: String = "",
    val rankKey: String = "",
    val grouping: String = "",
    /** The card's `_statusModified` at ranking time (link key); null on pre-link-key rows. */
    val cardStatusModified: Instant? = null
)
