package ru.citeck.ecos.uiserv.domain.form.service.resolver

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.config.lib.dto.ConfigValueType
import ru.citeck.ecos.config.lib.service.EcosConfigService
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import javax.annotation.PostConstruct

@Component
class ConfigFormResolver(
    val recordsService: RecordsService,
    val ecosFormService: EcosFormService,
    val ecosConfigService: EcosConfigService
) : EcosFormResolver {


    companion object {
        private val BUTTONS_CONFIG = DataValue.createObj()
            .set("type", "columns")
            .set("key", "buttons-columns")
            .set("columns", DataValue.createArr()
                .add(DataValue.createObj().set("md", 3).set("type", "column").set("input", false).set("index", 0))
                .add(DataValue.createObj().set("md", 3).set("type", "column").set("input", false).set("index", 1))
                .add(DataValue.createObj().set("md", 3).set("type", "column").set("input", false).set("index", 2)
                    .set("components", DataValue.createArr()
                        .add(DataValue.createObj()
                            .set("type", "button")
                            .set("key", "cancel")
                            .set("label", MLText(
                                I18nContext.RUSSIAN to "Отменить",
                                I18nContext.ENGLISH to "Cancel"
                            ))
                            .set("action", "event")
                            .set("event", "cancel")
                            .set("block", true)
                            .set("input", true)
                        )
                    )
                ).add(DataValue.createObj().set("md", 3).set("type", "column").set("input", false).set("index", 3)
                    .set("components", DataValue.createArr()
                        .add(DataValue.createObj()
                            .set("type", "button")
                            .set("theme", "primary")
                            .set("key", "submit")
                            .set("label", MLText(
                                I18nContext.RUSSIAN to "Сохранить",
                                I18nContext.ENGLISH to "Save"
                            ))
                            .set("block", true)
                            .set("input", true)
                        )
                    )
                )
            )

        val log = KotlinLogging.logger {}
    }

    @PostConstruct
    fun init() {
        ecosFormService.register(this)
    }

    override fun getFormModel(key: String): EcosFormModel? {

        val config = ecosConfigService.getConfig(key)
        val type = config.valueDef.type

        val model = EcosFormModel()
        model.width = "sm"
        model.definition = ObjectData.create()
            .set("components", createComponents(type,  config.valueDef.multiple))

        return model
    }

    private fun createComponents(type: ConfigValueType, multiple: Boolean): List<DataValue> {

        val simpleValueControl = DataValue.createObj()
            .set("label",
                MLText(
                    I18nContext.RUSSIAN to "Значение",
                    I18nContext.ENGLISH to "Value"
                )
            )
            .set("key", "_value")
            .set("type", "textfield")
            .set("input", true)
            .set("multiple", multiple)

        when (type) {
            ConfigValueType.TEXT -> {
                simpleValueControl["type"] = "textfield"
            }
            ConfigValueType.MLTEXT -> {
                simpleValueControl["type"] = "mlText"
            }
            ConfigValueType.NUMBER -> {
                simpleValueControl["type"] = "number"
            }
            ConfigValueType.BOOLEAN -> {
                simpleValueControl["type"] = "checkbox"
                simpleValueControl["label"] = MLText(
                    I18nContext.RUSSIAN to "Включить",
                    I18nContext.ENGLISH to "Enable"
                )
            }
            ConfigValueType.DATE -> {
                simpleValueControl["type"] = "datetime"
                simpleValueControl["enableTime"] = false
            }
            ConfigValueType.DATETIME -> {
                simpleValueControl["type"] = "datetime"
                simpleValueControl["enableTime"] = true
            }
            else -> {}
        }

        return listOf(simpleValueControl, BUTTONS_CONFIG)
    }

    override fun getType(): String {
        return "config"
    }
}
