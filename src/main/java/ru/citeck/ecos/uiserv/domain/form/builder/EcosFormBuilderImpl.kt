package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef

class EcosFormBuilderImpl(
    val context: FormBuilderContext,
) : EcosFormBuilder {

    private var components = emptyList<DataValue>()
    private val formModel = EcosFormDef.create()

    override fun withId(id: String): EcosFormBuilder {
        formModel.withId(id)
        return this
    }

    override fun withWidth(width: EcosFormWidth): EcosFormBuilder {
        formModel.withWidth(width.key)
        return this
    }

    override fun withWorkspace(workspace: String): EcosFormBuilder {
        formModel.withWorkspace(workspace)
        return this
    }

    override fun withTitle(title: MLText): EcosFormBuilder {
        formModel.withTitle(title)
        return this
    }

    override fun withComponents(action: (EcosFormComponentsBuilder) -> Unit): EcosFormBuilder {
        val builder = EcosFormComponentsBuilderImpl(context)
        action.invoke(builder)
        this.components = builder.getComponents()
        return this
    }

    override fun build(): EcosFormDef {
        formModel.definition = ObjectData.create()
            .set("components", components)
        return formModel.build()
    }
}
