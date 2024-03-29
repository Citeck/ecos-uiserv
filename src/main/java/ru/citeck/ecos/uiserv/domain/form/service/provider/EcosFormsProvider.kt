package ru.citeck.ecos.uiserv.domain.form.service.provider

import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef

interface EcosFormsProvider {

    fun getFormById(id: String): EntityWithMeta<EcosFormDef>?

    fun getType(): String
}
