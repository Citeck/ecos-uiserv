package ru.citeck.ecos.uiserv.domain.journal.registry

import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.webapp.api.entity.toEntityRef

class JournalRegistryRecordsAttMixin : AttMixin {

    companion object {
        private const val ATT_TYPE_REF = "typeRef"

        private val PROVIDED_ATTS = setOf(ATT_TYPE_REF)
    }

    override fun getAtt(path: String, value: AttValueCtx): Any? {
        return when (path) {
            ATT_TYPE_REF -> {
                val ref = value.getAtt(ATT_TYPE_REF).asText()
                if (ref.isNotBlank()) {
                    ref.toEntityRef()
                } else {
                    null
                }
            }
            else -> null
        }
    }

    override fun getProvidedAtts(): Collection<String> {
        return PROVIDED_ATTS
    }
}
