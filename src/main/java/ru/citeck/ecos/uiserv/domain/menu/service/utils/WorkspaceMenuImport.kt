package ru.citeck.ecos.uiserv.domain.menu.service.utils

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.listener.TypeInWsCreatedListener

/**
 * Recognizes an untouched workspace template with automatic type entries and carries those
 * entries into an imported menu. Customized menus and menus for other audiences are preserved.
 */
object WorkspaceMenuImport {

    @JvmStatic
    fun mergeAutomaticMenu(imported: MenuDto, candidate: MenuDto, template: MenuDto): MenuDto? {
        if (candidate.id == imported.id ||
            candidate.workspace != imported.workspace ||
            candidate.version != imported.version ||
            candidate.type != imported.type ||
            candidate.version != template.version ||
            candidate.type != template.type ||
            !isForEveryone(candidate) ||
            !isForEveryone(imported)
        ) {
            return null
        }

        val automaticItems = mutableListOf<MenuItemDef>()
        val stripped = candidate.subMenu.mapValues { (_, subMenu) ->
            copySubMenu(subMenu, stripAutomaticItems(subMenu.items, automaticItems))
        }
        if (automaticItems.isEmpty() || ObjectData.create(stripped) != ObjectData.create(template.subMenu)) {
            return null
        }

        val knownTypes = mutableSetOf<String>()
        collectAutomaticTypes(imported.subMenu.values.flatMap { it.items }, knownTypes)
        val missing = automaticItems.filter { knownTypes.add(automaticType(it.id)) }
        if (missing.isEmpty()) {
            return imported
        }

        val result = imported.copy().build()
        val left = result.subMenu["left"] ?: return null
        var inserted = false

        fun appendToSection(items: List<MenuItemDef>): List<MenuItemDef> {
            return items.map { item ->
                if (item.id == "sections" && !inserted) {
                    inserted = true
                    item.copy().withItems(item.items + missing).build()
                } else {
                    item.copy().withItems(appendToSection(item.items)).build()
                }
            }
        }

        left.items = appendToSection(left.items)
        return if (inserted) {
            result
        } else {
            null
        }
    }

    private fun isForEveryone(menu: MenuDto): Boolean {
        return menu.authorities.isEmpty() || menu.authorities == listOf(AuthGroup.EVERYONE)
    }

    private fun stripAutomaticItems(items: List<MenuItemDef>, removed: MutableList<MenuItemDef>): List<MenuItemDef> {
        return items.mapNotNull { item ->
            if (isAutomatic(item)) {
                removed.add(item)
                null
            } else {
                item.copy().withItems(stripAutomaticItems(item.items, removed)).build()
            }
        }
    }

    private fun collectAutomaticTypes(items: List<MenuItemDef>, result: MutableSet<String>) {
        for (item in items) {
            if (isAutomatic(item)) {
                result.add(automaticType(item.id))
            }
            collectAutomaticTypes(item.items, result)
        }
    }

    private fun isAutomatic(item: MenuItemDef): Boolean {
        return item.id.startsWith(TypeInWsCreatedListener.AUTO_JOURNAL_MENU_ITEM_ID_PREFIX) &&
            item.type in setOf("JOURNAL", "DOCLIB", "PREVIEW_LIST") &&
            item.items.isEmpty()
    }

    private fun automaticType(id: String): String {
        return id.substringBeforeLast('-')
    }

    private fun copySubMenu(source: SubMenuDef, items: List<MenuItemDef>): SubMenuDef {
        val result = SubMenuDef()
        result.config = source.config
        result.allowedFor = source.allowedFor
        result.items = items
        return result
    }
}
