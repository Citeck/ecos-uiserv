package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText

class EcosFormPanelBuilderImpl<T>(
    private val context: FormBuilderContext,
    private val buildImpl: (DataValue) -> T
) : EcosFormPanelBuilder<T> {

    private val data: DataValue = DataValue.createObj()

    override fun withKey(key: String): EcosFormPanelBuilder<T> {
        data[EcosFormComponentProps.KEY] = key
        return this
    }

    override fun withName(name: MLText): EcosFormPanelBuilder<T> {
        data[EcosFormComponentProps.NAME] = name
        return this
    }

    override fun withComponents(action: (EcosFormComponentsBuilder) -> Unit): EcosFormPanelBuilder<T> {
        val builder = EcosFormComponentsBuilderImpl(context)
        action.invoke(builder)
        data["components"] = builder.getComponents()
        return this
    }

    override fun build(): T {
        return buildImpl.invoke(data)
    }
}
