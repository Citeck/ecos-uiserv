package ru.citeck.ecos.uiserv.domain.admin.api.records.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData

data class AdminSectionDto(
    val name: MLText,
    val type: String,
    val config: ObjectData
) {
    open class Builder() {

        var name: MLText = MLText.EMPTY
        var type: String = ""
        var config: ObjectData = ObjectData.create()

        constructor(base: AdminSectionDto) : this() {
            this.name = base.name
            this.type = base.type
            this.config = base.config
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            return this
        }

        fun withType(type: String): Builder {
            this.type = type
            return this
        }

        fun withConfig(config: ObjectData?): Builder {
            this.config = config ?: ObjectData.create()
            return this
        }

        fun build(): AdminSectionDto {
            return AdminSectionDto(
                name,
                type,
                config
            )
        }
    }
}
