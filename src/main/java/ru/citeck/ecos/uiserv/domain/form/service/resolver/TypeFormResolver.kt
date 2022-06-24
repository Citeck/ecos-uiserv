package ru.citeck.ecos.uiserv.domain.form.service.resolver

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import javax.annotation.PostConstruct

@Component
class TypeFormResolver(
    val recordsService: RecordsService,
    val ecosFormService: EcosFormService,
    val typesRegistry: EcosTypesRegistry
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

        val typeDef = typesRegistry.getValue(key) ?: return null

        val model = EcosFormModel()
        model.width = "m"
        model.definition = ObjectData.create()
            .set("components", createComponents(typeDef.model.attributes))

        return model
    }

    private fun createComponents(attributes: List<AttributeDef>): List<DataValue> {
        val result = mutableListOf<DataValue>()
        attributes.mapNotNull {
            createComponent(it)
        }.forEach {
            result.add(it)
        }
        result.add(BUTTONS_CONFIG)
        return result
    }

    private fun createComponent(attribute: AttributeDef): DataValue? {

        val simpleValueControl = DataValue.createObj()
            .set("label", attribute.name)
            .set("key", "_value")
            .set("type", "textfield")
            .set("input", true)
            .set("multiple", attribute.multiple)

        when (attribute.type) {
            AttributeType.TEXT -> {
                simpleValueControl["type"] = "textfield"
            }
            AttributeType.MLTEXT -> {
                simpleValueControl["type"] = "mlText"
            }
            AttributeType.NUMBER -> {
                simpleValueControl["type"] = "number"
            }
            AttributeType.BOOLEAN -> {
                simpleValueControl["type"] = "checkbox"
                simpleValueControl["label"] = MLText(
                    I18nContext.RUSSIAN to "Включить",
                    I18nContext.ENGLISH to "Enable"
                )
            }
            AttributeType.DATE -> {
                simpleValueControl["type"] = "datetime"
                simpleValueControl["enableTime"] = false
            }
            AttributeType.DATETIME -> {
                simpleValueControl["type"] = "datetime"
                simpleValueControl["enableTime"] = true
            }
            else -> return null
        }

        return simpleValueControl
    }

    override fun getType(): String {
        return "type"
    }
}
