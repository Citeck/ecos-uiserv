package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText

open class EcosFormButtonBuilderImpl<T>(
    private val buildImpl: (DataValue) -> T
) : EcosFormButtonBuilder<T> {

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

    override fun withKey(key: String): EcosFormButtonBuilder<T> {
        data[KEY] = key
        return this
    }

    override fun withName(name: MLText): EcosFormButtonBuilder<T> {
        data[NAME] = name
        return this
    }

    override fun withProperty(key: String, value: String): EcosFormButtonBuilder<T> {
        val props = data[PROPERTIES]
        if (props.isNull()) {
            props[PROPERTIES] = DataValue.createObj()
        }
        props[key] = value
        return this
    }

    override fun build(): T {
        return buildImpl(data)
    }
}
