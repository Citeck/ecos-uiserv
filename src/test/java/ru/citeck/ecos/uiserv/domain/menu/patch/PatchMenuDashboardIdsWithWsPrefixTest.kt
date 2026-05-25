package ru.citeck.ecos.uiserv.domain.menu.patch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.UUID

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class PatchMenuDashboardIdsWithWsPrefixTest {

    @Autowired
    private lateinit var patch: PatchMenuDashboardIdsWithWsPrefix
    @Autowired
    private lateinit var menuService: MenuService
    @Autowired
    private lateinit var dashboardService: DashboardService
    @Autowired
    private lateinit var recordsService: RecordsService

    private val createdMenus = mutableListOf<IdInWs>()
    private val createdDashboardIds = mutableListOf<Pair<String, String>>() // extId, ws

    @BeforeEach
    fun setup() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            menuService.getAllMenus()
                .filter { !menuService.isDefaultMenu(it.id) }
                .forEach { menuService.delete(IdInWs.create(it.workspace, it.id)) }
        }
    }

    @AfterEach
    fun cleanup() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            createdMenus.forEach { id ->
                runCatching { menuService.delete(id) }
            }
            createdDashboardIds.forEach { (id, ws) ->
                runCatching { dashboardService.removeDashboard(id, ws) }
            }
        }
        createdMenus.clear()
        createdDashboardIds.clear()
    }

    @Test
    fun menuWithoutDashboardItemsIsNotModified() {
        val menuId = createMenu(workspace = "custom-workspace") {
            it["left"] = subMenu(
                MenuItemDef.create().withId("a").withType("SECTION").build()
            )
        }

        val status = runPatchAsSystem()

        assertThat(status.menusUpdated).isEqualTo(0)
        assertThat(status.itemsRewritten).isEqualTo(0)
        val after = getMenu(menuId)
        assertThat(after.subMenu["left"]?.items?.first()?.type).isEqualTo("SECTION")
    }

    @Test
    fun alreadyPrefixedDashboardIdIsNotModified() {
        val extId = uuid()
        createDashboard(extId, workspace = "custom-workspace")
        val prefixedRef = "uiserv/dashboard@custom-workspace:$extId"

        val menuId = createMenu(workspace = "custom-workspace") {
            it["left"] = subMenu(dashboardItem(prefixedRef))
        }

        val status = runPatchAsSystem()

        assertThat(status.itemsRewritten).isEqualTo(0)
        assertThat(dashboardId(getMenu(menuId))).isEqualTo(prefixedRef)
    }

    @Test
    fun bareIdResolvingToGlobalDashboardStaysBare() {
        val extId = uuid()
        createDashboard(extId, workspace = "")
        val bareRef = "uiserv/dashboard@$extId"

        val menuId = createMenu(workspace = "") {
            it["left"] = subMenu(dashboardItem(bareRef))
        }

        val status = runPatchAsSystem()

        assertThat(status.itemsRewritten).isEqualTo(0)
        assertThat(dashboardId(getMenu(menuId))).isEqualTo(bareRef)
    }

    @Test
    fun bareIdPointingToPersonalAdminDashboardIsRewrittenWithPrefix() {
        val extId = uuid()
        createDashboard(extId, workspace = "user__admin")
        val bareRef = "uiserv/dashboard@$extId"

        val menuId = createMenu(workspace = "") {
            it["left"] = subMenu(dashboardItem(bareRef))
        }

        val status = runPatchAsSystem()

        assertThat(status.itemsRewritten).isEqualTo(1)
        assertThat(dashboardId(getMenu(menuId)))
            .isEqualTo("uiserv/dashboard@user__admin:$extId")
    }

    @Test
    fun bareIdResolvesToDashboardInMenusOwnWorkspaceWhenMultipleCandidatesExist() {
        val extId = uuid()
        createDashboard(extId, workspace = "custom-workspace")
        createDashboard(extId, workspace = "user__admin")
        val bareRef = "uiserv/dashboard@$extId"

        val menuId = createMenu(workspace = "custom-workspace") {
            it["left"] = subMenu(dashboardItem(bareRef))
        }

        val status = runPatchAsSystem()

        assertThat(status.itemsRewritten).isEqualTo(1)
        assertThat(dashboardId(getMenu(menuId)))
            .isEqualTo("uiserv/dashboard@custom-workspace:$extId")
    }

    @Test
    fun bareIdWithNoDashboardInDbIsLeftUnchangedAndCountedAsNotFound() {
        val bareRef = "uiserv/dashboard@${uuid()}"

        val menuId = createMenu(workspace = "custom-workspace") {
            it["left"] = subMenu(dashboardItem(bareRef))
        }

        val status = runPatchAsSystem()

        assertThat(status.itemsRewritten).isEqualTo(0)
        assertThat(status.itemsNotFound).isEqualTo(1)
        assertThat(dashboardId(getMenu(menuId))).isEqualTo(bareRef)
    }

    @Test
    fun nestedDashboardItemInsideSectionIsRewritten() {
        val extId = uuid()
        createDashboard(extId, workspace = "user__admin")
        val bareRef = "uiserv/dashboard@$extId"

        val nested = dashboardItem(bareRef)
        val section = MenuItemDef.create()
            .withId("section-1")
            .withType("SECTION")
            .withItems(listOf(nested))
            .build()

        val menuId = createMenu(workspace = "") {
            it["left"] = subMenu(section)
        }

        val status = runPatchAsSystem()

        assertThat(status.itemsRewritten).isEqualTo(1)
        val sectionAfter = getMenu(menuId).subMenu["left"]!!.items.first()
        val nestedAfter = sectionAfter.items.first()
        assertThat(nestedAfter.config["dashboardId"].asText())
            .isEqualTo("uiserv/dashboard@user__admin:$extId")
    }

    @Test
    fun menusInDifferentWorkspacesAreProcessedIndependently() {
        val extIdA = uuid()
        val extIdB = uuid()
        createDashboard(extIdA, workspace = "custom-workspace")
        createDashboard(extIdB, workspace = "another-workspace")

        val menuA = createMenu(workspace = "custom-workspace") {
            it["left"] = subMenu(dashboardItem("uiserv/dashboard@$extIdA"))
        }
        val menuB = createMenu(workspace = "another-workspace") {
            it["left"] = subMenu(dashboardItem("uiserv/dashboard@$extIdB"))
        }

        val status = runPatchAsSystem()

        assertThat(status.itemsRewritten).isEqualTo(2)
        assertThat(dashboardId(getMenu(menuA)))
            .isEqualTo("uiserv/dashboard@custom-workspace:$extIdA")
        assertThat(dashboardId(getMenu(menuB)))
            .isEqualTo("uiserv/dashboard@another-workspace:$extIdB")
    }

    // ---- helpers ----

    private fun runPatchAsSystem(): PatchMenuDashboardIdsWithWsPrefix.Status = AuthContext.runAsSystem { patch.invoke() } as PatchMenuDashboardIdsWithWsPrefix.Status

    private fun createMenu(
        workspace: String,
        build: (MutableMap<String, SubMenuDef>) -> Unit
    ): IdInWs {
        val subMenu = mutableMapOf<String, SubMenuDef>()
        build(subMenu)
        val dto = MenuDto.create()
            .withId(uuid())
            .withType("LEFT_MENU")
            .withAuthorities(listOf("GROUP_EVERYONE"))
            .withWorkspace(workspace)
            .withSubMenu(subMenu)
            .build()
        val saved = AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) { menuService.save(dto) }
        val idInWs = IdInWs.create(saved.workspace, saved.id)
        createdMenus.add(idInWs)
        return idInWs
    }

    private fun getMenu(idInWs: IdInWs): MenuDto = menuService.getMenu(idInWs).orElseThrow { AssertionError("Menu $idInWs not found") }

    private fun createDashboard(extId: String, workspace: String) {
        val atts = ObjectData.create()
        atts["id"] = extId
        atts["workspace"] = workspace
        atts["typeRef"] = "emodel/type@user-dashboard"
        atts["config"] = "{\"k\":\"v\"}"
        val rec = RecordAtts()
        rec.setId(EntityRef.valueOf("dashboard@"))
        rec.setAttributes(atts)
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            recordsService.mutate(rec)
        }
        createdDashboardIds.add(extId to workspace)
    }

    private fun dashboardItem(dashboardId: String): MenuItemDef {
        val cfg = ObjectData.create()
        cfg["dashboardId"] = dashboardId
        return MenuItemDef.create()
            .withId(uuid())
            .withType("DASHBOARD")
            .withConfig(cfg)
            .build()
    }

    private fun subMenu(vararg items: MenuItemDef): SubMenuDef {
        val sub = SubMenuDef()
        sub.items = items.toMutableList()
        return sub
    }

    private fun dashboardId(menu: MenuDto): String = menu.subMenu["left"]!!.items.first().config["dashboardId"].asText()

    private fun uuid(): String = UUID.randomUUID().toString()
}
