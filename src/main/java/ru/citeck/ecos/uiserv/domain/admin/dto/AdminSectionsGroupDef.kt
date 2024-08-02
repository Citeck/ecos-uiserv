package ru.citeck.ecos.uiserv.domain.admin.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
@JsonDeserialize(builder = AdminSectionsGroupDef.Builder::class)
data class AdminSectionsGroupDef(
    val id: String,
    val name: MLText,
    val order: Float,
    val sections: List<AdminSectionDef>
) {
    companion object {

        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): AdminSectionsGroupDef {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): AdminSectionsGroupDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    fun withSections(sections: List<AdminSectionDef>?): AdminSectionsGroupDef {
        return copy().withSections(sections).build()
    }

    open class Builder() {

        var id: String = ""
        var name: MLText = MLText.EMPTY
        var order: Float = 0f
        var sections: List<AdminSectionDef> = emptyList()

        constructor(base: AdminSectionsGroupDef) : this() {
            this.id = base.id
            this.name = base.name
            this.order = base.order
            this.sections = base.sections
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            return this
        }

        fun withOrder(order: Float?): Builder {
            this.order = order ?: 0F
            return this
        }

        fun withSections(sections: List<AdminSectionDef>?): Builder {
            this.sections = sections ?: emptyList()
            return this
        }

        fun build(): AdminSectionsGroupDef {
            return AdminSectionsGroupDef(
                id,
                name,
                order,
                sections
            )
        }
    }
}
