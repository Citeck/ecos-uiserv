package ru.citeck.ecos.uiserv.domain.form.service.provider

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.config.lib.dto.ConfigValueDef
import ru.citeck.ecos.config.lib.dto.ConfigValueType
import ru.citeck.ecos.config.lib.service.EcosConfigService
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormBuilder
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormBuilderFactory
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormInputType
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormWidth
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import javax.annotation.PostConstruct

@Component
class ConfigFormsProvider(
    val recordsService: RecordsService,
    val ecosFormService: EcosFormService,
    val ecosConfigService: EcosConfigService,
    val formBuilderFactory: EcosFormBuilderFactory
) : EcosFormsProvider {

    @PostConstruct
    fun init() {
        ecosFormService.register(this)
    }

    override fun getFormById(id: String): EntityWithMeta<EcosFormDef>? {

        val config = ecosConfigService.getConfig(id)
        val formBuilder = formBuilderFactory.createBuilder()

        formBuilder.withWidth(EcosFormWidth.SMALL)
        addInput(formBuilder, config.valueDef)
        formBuilder.addCancelAndSubmitButtons()

        return EntityWithMeta(formBuilder.build())
    }

    private fun addInput(formBuilder: EcosFormBuilder, valueDef: ConfigValueDef) {

        val name = if (valueDef.type == ConfigValueType.BOOLEAN) {
            MLText(
                I18nContext.RUSSIAN to "Включить",
                I18nContext.ENGLISH to "Enable"
            )
        } else {
            MLText(
                I18nContext.RUSSIAN to "Значение",
                I18nContext.ENGLISH to "Value"
            )
        }

        val type = when (valueDef.type) {
            ConfigValueType.TEXT -> EcosFormInputType.TEXT_AREA
            ConfigValueType.MLTEXT -> EcosFormInputType.ML_TEXT_AREA
            ConfigValueType.NUMBER -> EcosFormInputType.NUMBER
            ConfigValueType.BOOLEAN -> EcosFormInputType.CHECKBOX
            ConfigValueType.DATE -> EcosFormInputType.DATE
            ConfigValueType.DATETIME -> EcosFormInputType.DATETIME
            ConfigValueType.ASSOC -> EcosFormInputType.JOURNAL
            else -> EcosFormInputType.TEXT_AREA
        }

        formBuilder.addInput(type, valueDef.config)
            .setKey("_value")
            .setMultiple(valueDef.multiple)
            .setName(name)
            .build()
    }

    override fun getType(): String {
        return "config"
    }
}
