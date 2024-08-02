package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesComponent
import ru.citeck.ecos.webapp.api.entity.EntityRef

class FormBuilderContextImpl(
    private val ecosTypesComponent: EcosTypesComponent
) : FormBuilderContext {

    override fun getJournalRefByType(typeRef: EntityRef): EntityRef {
        return EntityRef.valueOf(ecosTypesComponent.getJournalRefByType(typeRef))
    }
}
