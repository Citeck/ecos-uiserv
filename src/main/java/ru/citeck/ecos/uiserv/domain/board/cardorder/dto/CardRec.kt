package ru.citeck.ecos.uiserv.domain.board.cardorder.dto

import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

class CardRec(
    @AttName("?id") val ref: EntityRef = EntityRef.EMPTY,
    @AttName("_status?str") val status: String = "",
    @AttName("_created") val created: Instant = Instant.EPOCH
)
