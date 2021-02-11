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

    fun getTypeInfo(typeRef: RecordRef): EcosTypeInfo? {
        return typesConfig.getTypeInfo(typeRef)
    }
}
