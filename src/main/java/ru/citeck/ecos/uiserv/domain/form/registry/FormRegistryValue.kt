package ru.citeck.ecos.uiserv.domain.form.registry

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class FormRegistryValue(
    val id: String,
    val title: MLText,
    val description: MLText,
    val formKey: String,
    val typeRef: EntityRef,
    val system: Boolean,
) {
    companion object {
        private val TYPE = TypeUtils.getTypeRef("form")
    }

    fun getEcosType(): RecordRef = TYPE
}
