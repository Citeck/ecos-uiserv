package ru.citeck.ecos.uiserv.domain.action.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.webapp.api.entity.EntityRef

@IncludeNonDefault
@JsonDeserialize(builder = ActionConfirmDef.Builder::class)
data class ActionConfirmDef(
    val title: MLText,
    val message: MLText,
    val formRef: EntityRef,
    val formAttributes: ObjectData,
    val attributesMapping: Map<String, String>
) {
    companion object {

        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): ActionConfirmDef {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): ActionConfirmDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    open class Builder() {

        var title: MLText = MLText.EMPTY
        var message: MLText = MLText.EMPTY
        var formRef: EntityRef = EntityRef.EMPTY
        var formAttributes: ObjectData = ObjectData.create()
        var attributesMapping: Map<String, String> = emptyMap()

        constructor(base: ActionConfirmDef) : this() {
            this.title = base.title
            this.message = base.message
            this.formRef = base.formRef
            this.formAttributes = base.formAttributes.deepCopy()
            this.attributesMapping = DataValue.create(attributesMapping).asMap(String::class.java, String::class.java)
        }

        fun withTitle(title: MLText?): Builder {
            this.title = title ?: MLText.EMPTY
            return this
        }

        fun withMessage(message: MLText?): Builder {
            this.message = message ?: MLText.EMPTY
            return this
        }

        fun withFormRef(formRef: EntityRef?): Builder {
            this.formRef = formRef ?: EntityRef.EMPTY
            return this
        }

        fun withFormAttributes(formAttributes: ObjectData?): Builder {
            this.formAttributes = formAttributes ?: ObjectData.create()
            return this
        }

        fun withAttributesMapping(attributesMapping: Map<String, String>?): Builder {
            this.attributesMapping = attributesMapping ?: emptyMap()
            return this
        }

        fun build(): ActionConfirmDef {
            return ActionConfirmDef(title, message, formRef, formAttributes, attributesMapping)
        }
    }
}
