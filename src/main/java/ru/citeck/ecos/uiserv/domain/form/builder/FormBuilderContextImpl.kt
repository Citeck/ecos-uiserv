package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesConfig

class FormBuilderContextImpl(
    private val ecosTypesConfig: EcosTypesConfig
) : FormBuilderContext {

    override fun getJournalRefByType(typeRef: RecordRef): RecordRef {
        return ecosTypesConfig.getJournalRefByType(typeRef)
    }
}
