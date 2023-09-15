package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText

open class EcosFormButtonBuilderImpl(
    private val buildImpl: (DataValue) -> EcosFormBuilder
) : EcosFormButtonBuilder {

    companion object {
        private const val KEY = "key"
        private const val TYPE = "type"
        private const val NAME = "label"
        private const val PROPERTIES = "properties"
    }

    private val data = DataValue.createObj()

    init {
        data[TYPE] = "button"
    }

    override fun setKey(key: String): EcosFormButtonBuilder {
        data[KEY] = key
        return this
    }

    override fun setName(name: MLText): EcosFormButtonBuilder {
        data[NAME] = name
        return this
    }

    override fun setProperty(key: String, value: String): EcosFormButtonBuilder {
        val props = data[PROPERTIES]
        if (props.isNull()) {
            props[PROPERTIES] = DataValue.createObj()
        }
        props[key] = value
        return this
    }

    override fun build(): EcosFormBuilder {
        return buildImpl(data)
    }
}
