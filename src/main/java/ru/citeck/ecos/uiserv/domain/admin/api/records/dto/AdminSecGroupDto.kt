package ru.citeck.ecos.uiserv.domain.admin.api.records.dto

import ru.citeck.ecos.commons.data.MLText

data class AdminSecGroupDto(
    val id: String,
    val name: MLText,
    val order: Float,
    val sections: List<AdminSectionDto>
) {
    companion object {

        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): AdminSecGroupDto {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): AdminSecGroupDto {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    open class Builder() {

        var id: String = ""
        var name: MLText = MLText.EMPTY
        var order: Float = 0f
        var sections: List<AdminSectionDto> = emptyList()

        constructor(base: AdminSecGroupDto) : this() {
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

        fun withSections(list: List<AdminSectionDto>): Builder {
            this.sections = list
            return this
        }

        fun build(): AdminSecGroupDto {
            return AdminSecGroupDto(
                id,
                name,
                order,
                sections
            )
        }
    }
}
