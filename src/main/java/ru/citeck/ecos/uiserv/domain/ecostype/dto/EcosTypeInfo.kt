package ru.citeck.ecos.uiserv.domain.ecostype.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.request.RequestContext

class EcosTypeInfo(
    val id: String,
    val name: MLText?,
    val parentRef: RecordRef?,
    val formRef: RecordRef?,
    val journalRef: RecordRef?,
    val metaRecord: RecordRef?,
    val inhSourceId: String?,
    val parents: List<RecordRef>,
    val inhDashboardType: String?,
    val inhCreateVariants: List<CreateVariantDef>?,
    @AttName("resolvedModel?json")
    val model: TypeModelDef?,
    @AttName(RecordConstants.ATT_ACTIONS)
    val inhActions: List<RecordRef>?
) {
    @AttName("?disp")
    fun getDisplayName(): String {
        return MLText.getClosestValue(name, RequestContext.getLocale())
    }
}
