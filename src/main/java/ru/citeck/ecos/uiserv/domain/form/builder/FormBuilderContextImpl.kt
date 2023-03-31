package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesComponent

class FormBuilderContextImpl(
    private val ecosTypesComponent: EcosTypesComponent
) : FormBuilderContext {

    override fun getJournalRefByType(typeRef: RecordRef): RecordRef {
        return ecosTypesComponent.getJournalRefByType(typeRef)
    }
}
