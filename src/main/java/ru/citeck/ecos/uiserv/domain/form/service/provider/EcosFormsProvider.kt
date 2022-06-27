package ru.citeck.ecos.uiserv.domain.form.service.provider

import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel

interface EcosFormsProvider {

    fun getFormById(id: String): EcosFormModel?

    fun getType(): String
}
