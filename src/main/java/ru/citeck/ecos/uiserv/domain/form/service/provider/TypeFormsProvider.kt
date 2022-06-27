package ru.citeck.ecos.uiserv.domain.form.service.provider

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.computed.ComputedAttType
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormBuilder
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormBuilderFactory
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormWidth
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import javax.annotation.PostConstruct

@Component
class TypeFormsProvider(
    val recordsService: RecordsService,
    val ecosFormService: EcosFormService,
    val typesRegistry: EcosTypesRegistry,
    val formBuilderFactory: EcosFormBuilderFactory
) : EcosFormsProvider {

    companion object {
        val log = KotlinLogging.logger {}
    }

    @PostConstruct
    fun init() {
        ecosFormService.register(this)
    }

    override fun getFormById(id: String): EcosFormModel? {

        val typeDef = typesRegistry.getValue(id) ?: return null
        val formBuilder = formBuilderFactory.createBuilder()
        formBuilder.setWidth(EcosFormWidth.MEDIUM)

        typeDef.model.attributes.forEach {
            createInput(formBuilder, it)
        }
        formBuilder.addCancelAndSubmitButtons()

        return formBuilder.build()
    }

    private fun createInput(formBuilder: EcosFormBuilder, attribute: AttributeDef) {

        if (attribute.computed.type != ComputedAttType.NONE) {
            return
        }
        formBuilder.addInput(attribute.type, attribute.config)
            .setKey(attribute.id)
            .setName(attribute.name)
            .setMultiple(attribute.multiple)
            .build()
    }

    override fun getType(): String {
        return "type"
    }
}
