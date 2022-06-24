package ru.citeck.ecos.uiserv.domain.form.service.resolver

import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel

interface EcosFormResolver {

    fun getFormModel(key: String): EcosFormModel?

    fun getType(): String
}
