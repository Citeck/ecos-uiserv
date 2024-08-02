package ru.citeck.ecos.uiserv.domain.journal.registry

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class JournalRegistryValue(
    val id: String,
    val name: MLText,
    val sourceId: String,
    val typeRef: EntityRef,
    val system: Boolean,
) {
    companion object {
        private val TYPE = ModelUtils.getTypeRef("journal")
    }

    fun getEcosType(): EntityRef = TYPE
}
