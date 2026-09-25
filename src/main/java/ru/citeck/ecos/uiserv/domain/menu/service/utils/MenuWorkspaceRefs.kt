package ru.citeck.ecos.uiserv.domain.menu.service.utils

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Function

/**
 * Single source of truth for the set of workspace-scoped refs stored in a [MenuDto].
 *
 * [rewrite] applies [transform] to every non-empty ref held in the item `config` under one of
 * [REF_CONFIG_KEYS], for every item of every sub menu (left/create/user), recursively through
 * nested `items`, and returns a copy. Values that the transform leaves unchanged keep their original
 * text. Non-ref fields (incl. workspace) are left untouched — callers set workspace separately.
 *
 * Every site that converts menu refs to/from the `CURRENT_WS:` placeholder routes through here so
 * the covered set cannot drift between export and the import paths:
 *  - [ru.citeck.ecos.uiserv.domain.menu.eapps.MenuArtifactHandler] — deploy + listen/export;
 *  - `MenuRecords` — `?data` export and records-mutate / artifact-upload import.
 *
 * Workspace templates (`WorkspaceUiService`) deliberately keep menu refs as they are: a template
 * carries only uiserv menus and dashboards, not the artifacts the refs point to.
 */
object MenuWorkspaceRefs {

    /**
     * Item config keys holding a ref to a workspace-scoped artifact:
     *  - `recordRef` — JOURNAL, KANBAN, DOCLIB, WIKI, PREVIEW_LIST, LINK-CREATE-CASE (journal/type/board),
     *    EDIT_RECORD (record to edit);
     *  - `typeRef`, `variantTypeRef` — LINK-CREATE-CASE;
     *  - `formRef` — EDIT_RECORD;
     *  - `menuRef` — INCLUDE_MENU;
     *  - `dashboardId` — DASHBOARD (full `uiserv/dashboard@...` ref);
     *  - `processDef` — START_WORKFLOW.
     */
    @JvmField
    val REF_CONFIG_KEYS = listOf(
        "recordRef",
        "typeRef",
        "variantTypeRef",
        "formRef",
        "menuRef",
        "dashboardId",
        "processDef"
    )

    @JvmStatic
    fun rewrite(menu: MenuDto, transform: Function<EntityRef, EntityRef>): MenuDto {
        if (menu.subMenu.isEmpty()) {
            return menu
        }
        return menu.copy()
            .withSubMenu(menu.subMenu.mapValues { (_, subMenu) -> rewriteSubMenu(subMenu, transform) })
            .build()
    }

    private fun rewriteSubMenu(subMenu: SubMenuDef, transform: Function<EntityRef, EntityRef>): SubMenuDef {
        val result = SubMenuDef()
        result.config = subMenu.config
        result.allowedFor = subMenu.allowedFor
        result.items = rewriteItems(subMenu.items, transform)
        return result
    }

    private fun rewriteItems(
        items: List<MenuItemDef>?,
        transform: Function<EntityRef, EntityRef>
    ): List<MenuItemDef> {
        return items?.map { rewriteItem(it, transform) } ?: emptyList()
    }

    private fun rewriteItem(item: MenuItemDef, transform: Function<EntityRef, EntityRef>): MenuItemDef {

        val newItems = rewriteItems(item.items, transform)

        var newConfig: ObjectData? = null
        for (key in REF_CONFIG_KEYS) {
            val value = item.config[key]
            if (!value.isTextual()) {
                continue
            }
            val strValue = value.asText()
            if (strValue.isBlank()) {
                continue
            }
            val ref = EntityRef.valueOf(strValue)
            if (EntityRef.isEmpty(ref)) {
                continue
            }
            val newRef = transform.apply(ref)
            if (newRef != ref) {
                if (newConfig == null) {
                    newConfig = ObjectData.deepCopyOrNew(item.config)
                }
                newConfig[key] = newRef.toString()
            }
        }

        return item.copy()
            .withConfig(newConfig ?: item.config)
            .withItems(newItems)
            .build()
    }
}
