package ru.citeck.ecos.uiserv.domain.board.cardorder.dto

import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

class JournalInfo(
    @AttName("typeRef?id") val typeRef: EntityRef = EntityRef.EMPTY,
    @AttName("predicate?json") val predicate: Predicate = VoidPredicate.INSTANCE
)
