package ru.citeck.ecos.uiserv.domain.form.service.provider

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.computed.ComputedAttType
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormBuilder
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormBuilderFactory
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormWidth
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
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

        private val NAME_PREFIXES = mapOf(
            I18nContext.RUSSIAN to "Форма для ",
            I18nContext.ENGLISH to "Form for ",
        )
    }

    @PostConstruct
    fun init() {
        ecosFormService.register(this)
    }

    override fun getFormById(id: String): EcosFormDef? {
        val typeDef = typesRegistry.getValue(id) ?: return null
        return createFormDef(typeDef, true)
    }

    fun createFormDef(typeDef: TypeDef, withDefinition: Boolean): EcosFormDef {

        val name = MLText(
            typeDef.name.getValues().entries.associate {
                it.key to ((NAME_PREFIXES[it.key] ?: "") + it.value)
            }
        )

        val formBuilder = formBuilderFactory.createBuilder()
        formBuilder.withId("${getType()}$${typeDef.id}")
            .withWidth(EcosFormWidth.MEDIUM)
            .withTitle(name)

        if (withDefinition) {
            typeDef.model.attributes.forEach {
                createInput(formBuilder, it)
            }
            formBuilder.addCancelAndSubmitButtons()
        }

        return formBuilder.build()
    }

    private fun createInput(formBuilder: EcosFormBuilder, attribute: AttributeDef) {

        if (attribute.computed.type != ComputedAttType.NONE) {
            return
        }
        var name = attribute.name
        if (MLText.isEmpty(name)) {
            name = MLText(attribute.id)
        }
        formBuilder.addInput(attribute.type, attribute.config)
            .setKey(attribute.id)
            .setName(name)
            .setMultiple(attribute.multiple)
            .build()
    }

    override fun getType(): String {
        return "type"
    }
}
