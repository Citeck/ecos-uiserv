package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType

interface EcosFormComponentsBuilder {

    fun addInput(type: AttributeType, config: ObjectData): EcosFormInputBuilder<EcosFormComponentsBuilder>

    fun addInput(type: EcosFormInputType, config: ObjectData): EcosFormInputBuilder<EcosFormComponentsBuilder>

    fun addPanel(): EcosFormPanelBuilder<EcosFormComponentsBuilder>

    fun addCancelAndSubmitButtons(): EcosFormComponentsBuilder

    fun addButton(): EcosFormButtonBuilder<EcosFormComponentsBuilder>
}
