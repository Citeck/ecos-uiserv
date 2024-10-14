package ru.citeck.ecos.uiserv.domain.dashdoard.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.webapp.api.entity.EntityRef

@IncludeNonDefault
@JsonDeserialize(builder = DashboardDto.Builder::class)
data class DashboardDto(
    var id: String,
    var name: MLText,
    var typeRef: EntityRef,
    var appliedToRef: EntityRef,
    var authority: String,
    var scope: String,
    var priority: Float,
    var workspace: String,
    var config: ObjectData,
    var attributes: ObjectData
) {

    companion object {
        @JvmStatic
        fun create(): Builder {
            return Builder()
        }
    }

    @JsonPOJOBuilder
    open class Builder() {

        var id: String = ""
        var name: MLText = MLText.EMPTY
        var typeRef: EntityRef = EntityRef.EMPTY
        var appliedToRef: EntityRef = EntityRef.EMPTY
        var authority: String = ""
        var scope: String = ""
        var priority: Float = 0f
        var workspace: String = ""
        var config: ObjectData = ObjectData.create()
        var attributes: ObjectData = ObjectData.create()

        constructor(base: DashboardDto) : this() {
            id = base.id
            name = base.name
            typeRef = base.typeRef
            appliedToRef = base.appliedToRef
            authority = base.authority
            scope = base.scope
            priority = base.priority
            workspace = base.workspace
            config = base.config.deepCopy()
            attributes = base.attributes.deepCopy()
        }

        fun withId(id: String?): Builder {
            this.id = id ?: ""
            return this
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            return this
        }

        fun withTypeRef(typeRef: EntityRef?): Builder {
            this.typeRef = typeRef ?: EntityRef.EMPTY
            return this
        }

        fun withAppliedToRef(appliedToRef: EntityRef?): Builder {
            this.appliedToRef = appliedToRef ?: EntityRef.EMPTY
            return this
        }

        fun withAuthority(authority: String?): Builder {
            this.authority = authority ?: ""
            return this
        }

        fun withScope(scope: String?): Builder {
            this.scope = scope ?: ""
            return this
        }

        fun withPriority(priority: Float?): Builder {
            this.priority = priority ?: 0f
            return this
        }

        fun withWorkspace(workspace: String?): Builder {
            this.workspace = workspace ?: ""
            return this
        }

        fun withConfig(config: ObjectData?): Builder {
            this.config = config?.deepCopy() ?: ObjectData.create()
            return this
        }

        fun withAttributes(attributes: ObjectData?): Builder {
            this.attributes = attributes?.deepCopy() ?: ObjectData.create()
            return this
        }

        fun build(): DashboardDto {
            return DashboardDto(
                id = id,
                name = name,
                typeRef = typeRef,
                appliedToRef = appliedToRef,
                authority = authority,
                scope = scope,
                priority = priority,
                workspace = workspace,
                config = config,
                attributes = attributes
            )
        }
    }
}
