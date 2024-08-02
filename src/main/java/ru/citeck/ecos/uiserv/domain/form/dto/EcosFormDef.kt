package ru.citeck.ecos.uiserv.domain.form.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@IncludeNonDefault
@JsonDeserialize(builder = EcosFormDef.Builder::class)
data class EcosFormDef(
    val id: String,
    val formKey: String,
    val title: MLText,
    val description: MLText,
    val customModule: String,
    val typeRef: EntityRef,
    val width: String,
    val system: Boolean,
    val i18n: ObjectData,
    val definition: ObjectData,
    val attributes: ObjectData,

    @AttName("_notExists")
    val notExists: Boolean = true
) {

    companion object {

        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): EcosFormDef {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): EcosFormDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    @JsonPOJOBuilder
    open class Builder() {

        var id: String = ""
        var formKey: String = ""
        var title: MLText = MLText.EMPTY
        var description: MLText = MLText.EMPTY
        var customModule: String = ""
        var typeRef: EntityRef = EntityRef.EMPTY
        var width: String = ""
        var system: Boolean = false
        var i18n: ObjectData = ObjectData.create()
        var definition: ObjectData = ObjectData.create()
        var attributes: ObjectData = ObjectData.create()

        constructor(base: EcosFormDef) : this() {
            id = base.id
            formKey = base.formKey
            title = base.title
            description = base.description
            customModule = base.customModule
            typeRef = base.typeRef
            width = base.width
            system = base.system
            i18n = base.i18n.deepCopy()
            definition = base.definition.deepCopy()
            attributes = base.attributes.deepCopy()
        }

        fun withId(id: String?): Builder {
            this.id = id ?: ""
            return this
        }

        fun withFormKey(formKey: String?): Builder {
            this.formKey = formKey ?: EMPTY.formKey
            return this
        }

        fun withTitle(title: MLText?): Builder {
            this.title = title ?: EMPTY.title
            return this
        }

        fun withDescription(description: MLText?): Builder {
            this.description = description ?: EMPTY.description
            return this
        }

        fun withCustomModule(customModule: String?): Builder {
            this.customModule = customModule ?: EMPTY.customModule
            return this
        }

        fun withTypeRef(typeRef: EntityRef?): Builder {
            this.typeRef = typeRef ?: EMPTY.typeRef
            return this
        }

        fun withWidth(width: String?): Builder {
            this.width = width ?: EMPTY.width
            return this
        }

        fun withSystem(system: Boolean?): Builder {
            this.system = system ?: EMPTY.system
            return this
        }

        fun withI18n(i18n: ObjectData?): Builder {
            this.i18n = i18n ?: ObjectData.create()
            return this
        }

        fun withDefinition(definition: ObjectData?): Builder {
            this.definition = definition ?: ObjectData.create()
            return this
        }

        fun withAttributes(attributes: ObjectData?): Builder {
            this.attributes = attributes ?: ObjectData.create()
            return this
        }

        fun build(): EcosFormDef {

            return EcosFormDef(
                id = id,
                formKey = formKey,
                title = title,
                description = description,
                customModule = customModule,
                typeRef = typeRef,
                width = width,
                system = system,
                i18n = i18n,
                definition = definition,
                attributes = attributes,
                notExists = id.isEmpty()
            )
        }
    }
}
