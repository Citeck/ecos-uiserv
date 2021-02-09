package ru.citeck.ecos.uiserv.domain.ecostype.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.uiserv.domain.journal.dto.CreateVariantDef

class EcosTypeInfo(
    val id: String,
    val name: MLText?,
    val parentRef: RecordRef?,
    val formRef: RecordRef?,
    val journalRef: RecordRef?,
    val sourceId: String?,
    val dashboardType: String?,
    val createVariants: List<CreateVariantDef>?,
    @AttName("model?json")
    val model: TypeModelDef?
) {
    @AttName("?disp")
    fun getDisplayName(): String {
        return MLText.getClosestValue(name, RequestContext.getLocale())
    }
}
