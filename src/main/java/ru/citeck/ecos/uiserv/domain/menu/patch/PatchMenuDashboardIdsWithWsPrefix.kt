package ru.citeck.ecos.uiserv.domain.menu.patch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.uiserv.domain.dashdoard.repo.DashboardEntity
import ru.citeck.ecos.uiserv.domain.dashdoard.repo.DashboardRepository
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatch

@Component
@EcosPatch("patch-menu-dashboard-ids-with-ws-prefix", "2026-05-22T00:00:00Z")
class PatchMenuDashboardIdsWithWsPrefix(
    private val menuService: MenuService,
    private val dashboardRepository: DashboardRepository,
    private val workspaceService: WorkspaceService
) : Function0<Any?> {

    companion object {
        private val log = KotlinLogging.logger {}

        private const val DASHBOARD_ITEM_TYPE = "DASHBOARD"
        private const val DASHBOARD_ID_KEY = "dashboardId"
        private const val DASHBOARD_SOURCE_ID = "dashboard"
        private const val DEFAULT_WORKSPACE_ID = "default"
    }

    override fun invoke(): Any {
        val status = Status()
        val allMenus = menuService.getAllMenus()
        status.menusScanned = allMenus.size
        log.info { "Scanning ${allMenus.size} menus for legacy DASHBOARD items" }

        for (menu in allMenus) {
            val menuStatus = MenuVisitStatus()
            val newSubMenu = menu.subMenu.mapValues { (_, subMenu) ->
                rewriteSubMenu(subMenu, menu.workspace, menuStatus)
            }
            status.itemsScanned += menuStatus.itemsScanned
            status.itemsRewritten += menuStatus.itemsRewritten
            status.itemsNotFound += menuStatus.itemsNotFound

            if (menuStatus.itemsRewritten > 0) {
                val updated = menu.copy().withSubMenu(newSubMenu).build()
                menuService.save(updated)
                status.menusUpdated++
                log.info {
                    "Updated menu '${menu.id}' (ws='${menu.workspace}'): " +
                        "rewritten ${menuStatus.itemsRewritten} item(s)"
                }
            }
        }

        log.info {
            "Patch finished: scanned ${status.menusScanned} menus / " +
                "${status.itemsScanned} DASHBOARD items, " +
                "rewrote ${status.itemsRewritten}, " +
                "not-found ${status.itemsNotFound}, " +
                "updated ${status.menusUpdated} menus"
        }
        return status
    }

    private fun rewriteSubMenu(
        subMenu: SubMenuDef,
        menuWorkspace: String,
        menuStatus: MenuVisitStatus
    ): SubMenuDef {
        val result = SubMenuDef()
        result.config = subMenu.config
        result.allowedFor = subMenu.allowedFor
        result.items = subMenu.items.map { rewriteItem(it, menuWorkspace, menuStatus) }
        return result
    }

    private fun rewriteItem(
        item: MenuItemDef,
        menuWorkspace: String,
        menuStatus: MenuVisitStatus
    ): MenuItemDef {
        val newChildren = item.items.map { rewriteItem(it, menuWorkspace, menuStatus) }

        if (item.type != DASHBOARD_ITEM_TYPE) {
            return if (newChildren === item.items) {
                item
            } else {
                item.copy().withItems(newChildren).build()
            }
        }

        menuStatus.itemsScanned++
        val storedId = item.config[DASHBOARD_ID_KEY].asText()
        val rewritten = resolveDashboardId(storedId, menuWorkspace, menuStatus)
        if (rewritten == null || rewritten == storedId) {
            return if (newChildren === item.items) {
                item
            } else {
                item.copy().withItems(newChildren).build()
            }
        }

        val newConfig = ObjectData.deepCopyOrNew(item.config)
        newConfig[DASHBOARD_ID_KEY] = rewritten
        menuStatus.itemsRewritten++
        return item.copy()
            .withConfig(newConfig)
            .withItems(newChildren)
            .build()
    }

    private fun resolveDashboardId(
        storedId: String,
        menuWorkspace: String,
        menuStatus: MenuVisitStatus
    ): String? {
        if (storedId.isBlank()) return null
        val ref = EntityRef.valueOf(storedId)
        if (ref.getSourceId() != DASHBOARD_SOURCE_ID) return null
        val localId = ref.getLocalId()
        if (localId.isBlank()) return null

        val idInWs = workspaceService.convertToIdInWs(localId)
        if (idInWs.workspace.isNotEmpty()) {
            // already WS-prefixed — idempotent skip
            return null
        }

        val extId = idInWs.id
        val candidates = dashboardRepository.findAllByExtId(extId)
        if (candidates.isEmpty()) {
            menuStatus.itemsNotFound++
            log.warn {
                "DASHBOARD menu item references missing dashboard extId='$extId' " +
                    "(menu ws='$menuWorkspace'); leaving ref as is"
            }
            return null
        }

        val chosen = pickCandidate(candidates, menuWorkspace)
        val newLocalId = workspaceService.addWsPrefixToId(extId, chosen.workspace.orEmpty())
        return EntityRef.create(AppName.UISERV, DASHBOARD_SOURCE_ID, newLocalId).toString()
    }

    private fun pickCandidate(candidates: List<DashboardEntity>, menuWorkspace: String): DashboardEntity {
        val normMenuWs = normalizeWs(menuWorkspace)

        candidates.firstOrNull { normalizeWs(it.workspace) == normMenuWs }?.let { return it }
        candidates.firstOrNull { normalizeWs(it.workspace).isEmpty() }?.let { return it }

        val sorted = candidates.sortedBy { normalizeWs(it.workspace) }
        if (sorted.size > 1) {
            log.warn {
                "Multiple dashboard candidates for extId='${sorted.first().extId}' " +
                    "across workspaces ${sorted.map { it.workspace }}; picking first by stable sort"
            }
        }
        return sorted.first()
    }

    private fun normalizeWs(workspace: String?): String {
        if (workspace.isNullOrBlank()) return ""
        if (workspace == DEFAULT_WORKSPACE_ID) return ""
        return workspace
    }

    private class MenuVisitStatus(
        var itemsScanned: Int = 0,
        var itemsRewritten: Int = 0,
        var itemsNotFound: Int = 0
    )

    class Status(
        var menusScanned: Int = 0,
        var itemsScanned: Int = 0,
        var itemsRewritten: Int = 0,
        var itemsNotFound: Int = 0,
        var menusUpdated: Int = 0
    )
}
