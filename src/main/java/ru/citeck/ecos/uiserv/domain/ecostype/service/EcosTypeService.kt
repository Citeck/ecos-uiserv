package ru.citeck.ecos.uiserv.domain.ecostype.service

import org.springframework.stereotype.Service
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesConfig
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo

@Service
class EcosTypeService(
    private val typesConfig: EcosTypesConfig
) {

    fun getTypeRefByJournal(journalRef: RecordRef): RecordRef {
        return typesConfig.getTypeRefByJournal(journalRef)
    }

    fun getJournalRefByTypeRef(typeRef: RecordRef): RecordRef {

        var journalRef = typesConfig.getJournalRefByType(typeRef)
        if (RecordRef.isEmpty(journalRef)) {
            val parents = typesConfig.getTypeInfo(typeRef)?.parents ?: emptyList()
            for (parentRef in parents) {
                if (RecordRef.isNotEmpty(parentRef)) {
                    journalRef = typesConfig.getJournalRefByType(parentRef)
                    if (RecordRef.isNotEmpty(journalRef)) {
                        return journalRef
                    }
                }
            }
        }

        return RecordRef.EMPTY
    }

    fun getTypeInfo(typeRef: RecordRef): EcosTypeInfo? {
        return typesConfig.getTypeInfo(typeRef)
    }
}
