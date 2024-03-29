package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType

class EcosFormComponentsBuilderImpl(private val context: FormBuilderContext) : EcosFormComponentsBuilder {

    companion object {
        private val CANCEL_AND_SUBMIT_BUTTONS = DataValue.createObj()
            .set("type", "columns")
            .set("key", "buttons-columns")
            .set(
                "columns",
                DataValue.createArr()
                    .add(DataValue.createObj().set("md", 3).set("type", "column").set("input", false).set("index", 0))
                    .add(DataValue.createObj().set("md", 3).set("type", "column").set("input", false).set("index", 1))
                    .add(
                        DataValue.createObj().set("md", 3).set("type", "column").set("input", false).set("index", 2)
                            .set(
                                "components",
                                DataValue.createArr()
                                    .add(
                                        DataValue.createObj()
                                            .set("type", "button")
                                            .set("key", "cancel")
                                            .set(
                                                "label",
                                                MLText(
                                                    I18nContext.RUSSIAN to "Отменить",
                                                    I18nContext.ENGLISH to "Cancel"
                                                )
                                            )
                                            .set("action", "event")
                                            .set("event", "cancel")
                                            .set("block", true)
                                            .set("input", true)
                                    )
                            )
                    ).add(
                        DataValue.createObj().set("md", 3).set("type", "column").set("input", false).set("index", 3)
                            .set(
                                "components",
                                DataValue.createArr()
                                    .add(
                                        DataValue.createObj()
                                            .set("type", "button")
                                            .set("theme", "primary")
                                            .set("key", "submit")
                                            .set(
                                                "label",
                                                MLText(
                                                    I18nContext.RUSSIAN to "Сохранить",
                                                    I18nContext.ENGLISH to "Save"
                                                )
                                            )
                                            .set("block", true)
                                            .set("input", true)
                                    )
                            )
                    )
            )
    }

    private val components = mutableListOf<DataValue>()

    override fun addInput(type: AttributeType, config: ObjectData): EcosFormInputBuilder<EcosFormComponentsBuilder> {
        return addInput(EcosFormInputType.getFromAttributeType(type), config)
    }

    override fun addInput(type: EcosFormInputType, config: ObjectData): EcosFormInputBuilder<EcosFormComponentsBuilder> {
        return EcosFormInputBuilderImpl(type, config, context) {
            components.add(it)
            this
        }
    }

    override fun addButton(): EcosFormButtonBuilder<EcosFormComponentsBuilder> {
        return EcosFormButtonBuilderImpl {
            components.add(it)
            this
        }
    }

    override fun addCancelAndSubmitButtons(): EcosFormComponentsBuilder {
        components.add(CANCEL_AND_SUBMIT_BUTTONS)
        return this
    }

    override fun addPanel(): EcosFormPanelBuilder<EcosFormComponentsBuilder> {
        return EcosFormPanelBuilderImpl(context) {
            components.add(it)
            this
        }
    }

    fun getComponents(): List<DataValue> {
        return components
    }
}
