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
    /** The card's `_statusModified` at ranking time (link key); a row without it is stale. */
    val cardStatusModified: Instant? = null,
    /** When the column's ordering was last curated via this row (the new-block anchor source). */
    val orderedAt: Instant? = null
)
