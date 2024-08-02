package ru.citeck.ecos.uiserv.domain.form.service.provider

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.model.lib.attributes.dto.computed.ComputedAttType
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormBuilderFactory
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormComponentsBuilder
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormWidth
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry

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

    override fun getFormById(id: String): EntityWithMeta<EcosFormDef>? {
        return getFormById(id, true)
    }

    fun getFormById(id: String, withDefinition: Boolean): EntityWithMeta<EcosFormDef>? {
        val typeDef = typesRegistry.getValueWithMeta(id) ?: return null
        return createFormDef(typeDef, withDefinition)
    }

    private fun createFormDef(typeDef: EntityWithMeta<TypeDef>, withDefinition: Boolean): EntityWithMeta<EcosFormDef> {

        val formBuilder = formBuilderFactory.createBuilder()
        formBuilder.withId("${getType()}$${typeDef.entity.id}")
            .withWidth(EcosFormWidth.MEDIUM)
            .withTitle(typeDef.entity.name)

        val form = formBuilder.withComponents { formComponents ->
            if (withDefinition) {
                typeDef.entity.model.attributes.forEach {
                    createInput(formComponents, it)
                }
                formComponents.addCancelAndSubmitButtons()
            }
        }.build()

        return EntityWithMeta(form, typeDef.meta)
    }

    private fun createInput(formBuilder: EcosFormComponentsBuilder, attribute: AttributeDef) {

        if (attribute.computed.type != ComputedAttType.NONE) {
            return
        }
        var name = attribute.name
        if (MLText.isEmpty(name)) {
            name = MLText(attribute.id)
        }
        val fieldKey = if (attribute.type == AttributeType.CONTENT && attribute.id == "content") {
            RecordConstants.ATT_CONTENT
        } else {
            attribute.id
        }
        formBuilder.addInput(attribute.type, attribute.config)
            .withKey(fieldKey)
            .withName(name)
            .withMultiple(attribute.multiple)
            .withMandatory(attribute.mandatory)
            .build()
    }

    override fun getType(): String {
        return "type"
    }
}
