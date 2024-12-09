package ru.citeck.ecos.uiserv.domain.workspace.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.io.file.mem.EcosMemDir
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import java.util.*

@Service
class WorkspaceUiService(
    private val menuService: MenuService,
    private val dashboardService: DashboardService
) {

    companion object {
        private val log = KotlinLogging.logger {}

        val REQUIRED_WS_ARTIFACTS_FROM_TEMPLATE = listOf("ui/**")
    }

    fun getWsArtifactsForTemplate(workspace: String): EcosMemDir {
        val artifacts = EcosMemDir()

        writeWsMenus(workspace, artifacts)
        writeWsDashboards(workspace, artifacts)

        return artifacts
    }

    fun deployWsArtifactsFromTemplate(workspace: String, artifacts: EcosMemDir) {
        deployWsMenus(workspace, artifacts)
        deployWsDashboards(workspace, artifacts)
    }

    private fun deployWsDashboards(workspace: String, outDir: EcosMemDir) {
        outDir.findFiles("ui/dashboard/**.json").forEach { dashboardFile ->
            val dashboardDto = dashboardFile.read {
                Json.mapper.readNotNull(it, DashboardDto::class.java)
            }!!
            val newId = UUID.randomUUID().toString()
            log.info { "Deploy dashboard $newId for workspace $workspace" }
            dashboardService.saveDashboard(
                dashboardDto.copy()
                    .withId(newId)
                    .withWorkspace(workspace)
                    .build()
            )
        }
    }

    private fun deployWsMenus(workspace: String, outDir: EcosMemDir) {
        outDir.findFiles("ui/menu/**.json").forEach { menuFile ->

            val menuDto = menuFile.read {
                Json.mapper.readNotNull(it, MenuDto::class.java)
            }!!

            val newId = UUID.randomUUID().toString()
            log.info { "Deploy menu $newId for workspace $workspace" }

            menuService.save(
                menuDto.copy()
                    .withId(newId)
                    .withWorkspace(workspace)
                    .build()
            )
        }
    }

    private fun writeWsMenus(workspace: String, outDir: EcosMemDir) {
        val menus = menuService.findAllForWorkspace(workspace)
        if (menus.isEmpty()) {
            return
        }
        val menuDir = outDir.createDir("ui/menu")
        for (menu in menus) {
            menuDir.createFile(menu.id + ".json", Json.mapper.toPrettyStringNotNull(menu))
        }
    }

    private fun writeWsDashboards(workspace: String, outDir: EcosMemDir) {
        val dashboards = dashboardService.findAllForWorkspace(workspace)
        if (dashboards.isEmpty()) {
            return
        }
        val dashboardsDir = outDir.createDir("ui/dashboard")
        for (dashboard in dashboards) {
            dashboardsDir.createFile(dashboard.id + ".json", Json.mapper.toPrettyStringNotNull(dashboard))
        }
    }
}
