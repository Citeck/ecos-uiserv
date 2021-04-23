package ru.citeck.ecos.uiserv.domain.menu.dto

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
data class MenuItemActionDef(
    val type: String,
    val config: ObjectData
) {
    companion object {
        val EMPTY = MenuItemActionDef("", ObjectData.create())
    }
}
