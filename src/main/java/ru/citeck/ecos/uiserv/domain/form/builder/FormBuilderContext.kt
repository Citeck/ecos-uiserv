package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.webapp.api.entity.EntityRef

interface FormBuilderContext {

    fun getJournalRefByType(typeRef: EntityRef): EntityRef
}
