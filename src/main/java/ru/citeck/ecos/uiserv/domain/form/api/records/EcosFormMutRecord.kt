package ru.citeck.ecos.uiserv.domain.form.api.records

import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef

class EcosFormMutRecord(other: EcosFormDef) : EcosFormDef.Builder(other) {

    val originalId = other.id

    constructor() : this(EcosFormDef.EMPTY)

    fun withModuleId(value: String?) {
        withId(value)
    }
}
