package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel

interface EcosFormBuilder {

    fun setId(id: String): EcosFormBuilder

    fun setWidth(width: EcosFormWidth): EcosFormBuilder

    fun addInput(type: AttributeType, config: ObjectData): EcosFormInputBuilder

    fun addInput(type: EcosFormInputType, config: ObjectData): EcosFormInputBuilder

    fun addCancelAndSubmitButtons(): EcosFormBuilder

    fun build(): EcosFormModel
}
