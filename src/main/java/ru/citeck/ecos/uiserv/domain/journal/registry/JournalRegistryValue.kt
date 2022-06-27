package ru.citeck.ecos.uiserv.domain.journal.registry

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records2.RecordRef

data class JournalRegistryValue(
    val id: String,
    val name: MLText,
    val sourceId: String,
    val typeRef: RecordRef,
    val system: Boolean,
) {
    companion object {
        private val TYPE = TypeUtils.getTypeRef("journal")
    }

    fun getEcosType(): RecordRef = TYPE
}
