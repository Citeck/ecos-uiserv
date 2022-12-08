package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef

interface EcosFormBuilder {

    fun withId(id: String): EcosFormBuilder

    fun withWidth(width: EcosFormWidth): EcosFormBuilder

    fun withTitle(title: MLText): EcosFormBuilder

    fun addInput(type: AttributeType, config: ObjectData): EcosFormInputBuilder

    fun addInput(type: EcosFormInputType, config: ObjectData): EcosFormInputBuilder

    fun addCancelAndSubmitButtons(): EcosFormBuilder

    fun build(): EcosFormDef
}
