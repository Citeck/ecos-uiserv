package ru.citeck.ecos.uiserv.domain.menu.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.uiserv.app.common.utils.WorkspaceRefUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef

@IncludeNonDefault
@JsonDeserialize(builder = MenuDto.Builder::class)
open class MenuDto(
    val id: String,
    val type: String,
    val authorities: List<String>,
    val version: Int,
    val workspace: String,
    val subMenu: Map<String, SubMenuDef>
) {
    companion object {
        @JvmField
        val EMPTY = create().build()

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }
    }

    @JsonIgnore
    fun getWorkspaceRef(): EntityRef {
        return WorkspaceRefUtils.getWorkspaceRefOrDefault(workspace)
    }

    fun copy(): Builder {
        return Builder(this)
    }

    @JsonPOJOBuilder
    open class Builder() {

        var id: String = ""
        var type: String = ""
        var authorities: List<String> = emptyList()
        var version: Int = 0
        var workspace: String = ""
        var subMenu: Map<String, SubMenuDef> = emptyMap()

        constructor(base: MenuDto) : this() {
            this.id = base.id
            this.type = base.type
            this.authorities = base.authorities
            this.version = base.version
            this.workspace = base.workspace
            this.subMenu = DataValue.create(base.subMenu).asMap(String::class.java, SubMenuDef::class.java)
        }

        fun withId(id: String?): Builder {
            this.id = id ?: ""
            return this
        }

        fun withType(type: String?): Builder {
            this.type = type ?: ""
            return this
        }

        fun withAuthorities(authorities: List<String>?): Builder {
            this.authorities = authorities ?: emptyList()
            return this
        }

        fun withVersion(version: Int?): Builder {
            this.version = version ?: EMPTY.version
            return this
        }

        fun withWorkspace(workspace: String?): Builder {
            this.workspace = workspace ?: ""
            return this
        }

        open fun withSubMenu(subMenu: Map<String, SubMenuDef>?): Builder {
            this.subMenu = subMenu ?: emptyMap()
            return this
        }

        fun build(): MenuDto {
            return MenuDto(
                id = id,
                type = type,
                authorities = authorities,
                version = version,
                workspace = workspace,
                subMenu = subMenu
            )
        }
    }
}
