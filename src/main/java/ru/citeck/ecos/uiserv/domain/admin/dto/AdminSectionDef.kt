package ru.citeck.ecos.uiserv.domain.admin.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
@JsonDeserialize(builder = AdminSectionDef.Builder::class)
data class AdminSectionDef(
    val name: MLText,
    val shortName: MLText,
    val type: String,
    val config: ObjectData
) {
    companion object {

        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): AdminSectionDef {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): AdminSectionDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    fun withName(name: MLText?): AdminSectionDef {
        return copy().withName(name).build()
    }

    fun withName(name: String?): AdminSectionDef {
        return copy().withName(MLText(name ?: "")).build()
    }

    open class Builder() {

        var name: MLText = MLText.EMPTY
        var shortName: MLText = MLText.EMPTY
        var type: String = ""
        var config: ObjectData = ObjectData.create()

        constructor(base: AdminSectionDef) : this() {
            this.name = base.name
            this.shortName = base.shortName
            this.type = base.type
            this.config = base.config
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            return this
        }

        fun withShortName(shortName: MLText?): Builder {
            this.shortName = shortName ?: MLText.EMPTY
            return this
        }

        fun withType(type: String?): Builder {
            this.type = type ?: ""
            return this
        }

        fun withConfig(config: ObjectData?): Builder {
            this.config = config ?: ObjectData.create()
            return this
        }

        fun build(): AdminSectionDef {
            return AdminSectionDef(
                name,
                shortName,
                type,
                config
            )
        }
    }
}
