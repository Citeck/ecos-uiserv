package ru.citeck.ecos.uiserv.domain.menu.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.webapp.api.entity.EntityRef

@IncludeNonDefault
@JsonDeserialize(builder = MenuItemDef.Builder::class)
data class MenuItemDef(
    val id: String,
    val label: MLText,
    val icon: EntityRef,
    val hidden: Boolean,
    val type: String,
    val config: ObjectData,
    val action: MenuItemActionDef,
    val items: List<MenuItemDef>,
    val collapsed: Boolean?,
    val allowedFor: List<String>
) {

    companion object {
        @JvmField
        val EMPTY = create().build()

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    class Builder() {

        var id: String = ""
        var label: MLText = MLText.EMPTY
        var icon: EntityRef = EntityRef.EMPTY
        var hidden: Boolean = false
        var type: String = ""
        var config: ObjectData = ObjectData.create()
        var action: MenuItemActionDef = MenuItemActionDef.EMPTY
        var items: List<MenuItemDef> = emptyList()
        var collapsed: Boolean? = null
        var allowedFor: List<String> = emptyList()

        constructor(base: MenuItemDef) : this() {
            this.id = base.id
            this.label = base.label
            this.icon = base.icon
            this.hidden = base.hidden
            this.type = base.type
            this.config = ObjectData.deepCopyOrNew(base.config)
            this.action = base.action
            this.items = DataValue.create(base.items).asList(MenuItemDef::class.java)
            this.collapsed = base.collapsed
            this.allowedFor = base.allowedFor
        }

        fun withId(id: String?): Builder {
            this.id = id ?: ""
            return this
        }

        fun withLabel(label: MLText?): Builder {
            this.label = label ?: MLText.EMPTY
            return this
        }

        fun withIcon(icon: EntityRef?): Builder {
            this.icon = icon ?: EntityRef.EMPTY
            return this
        }

        fun withHidden(hidden: Boolean?): Builder {
            this.hidden = hidden == true
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

        fun withAction(action: MenuItemActionDef?): Builder {
            this.action = action ?: MenuItemActionDef.EMPTY
            return this
        }

        fun withItems(items: List<MenuItemDef>?): Builder {
            this.items = items ?: emptyList()
            return this
        }

        fun withCollapsed(collapsed: Boolean?): Builder {
            this.collapsed = collapsed
            return this
        }

        fun withAllowedFor(allowedFor: List<String>?): Builder {
            this.allowedFor = allowedFor ?: emptyList()
            return this
        }

        fun build(): MenuItemDef {
            return MenuItemDef(
                id,
                label,
                icon,
                hidden,
                type,
                config,
                action,
                items,
                collapsed,
                allowedFor
            )
        }
    }
}
