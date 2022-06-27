package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.records2.RecordRef

interface FormBuilderContext {

    fun getJournalRefByType(typeRef: RecordRef): RecordRef
}
