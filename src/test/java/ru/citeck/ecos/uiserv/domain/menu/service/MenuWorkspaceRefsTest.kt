package ru.citeck.ecos.uiserv.domain.menu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.citeck.ecos.apps.app.domain.handler.ArtifactDeployMeta
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDeployArtifact
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.testutils.MenuTestBase
import ru.citeck.ecos.uiserv.domain.menu.service.utils.MenuWorkspaceRefs
import ru.citeck.ecos.uiserv.domain.workspace.service.WorkspaceUiService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * COREDEV-580: ws-scoped refs in menu items become `CURRENT_WS:` placeholders on export
 * and are bound to the target workspace on import.
 *
 * WorkspaceApiMock maps workspace id == workspace system id, so a `ws-a:` prefix means workspace `ws-a`.
 */
class MenuWorkspaceRefsTest : MenuTestBase() {

    companion object {
        private const val WS_A = "ws-a"
        private const val WS_B = "ws-b"
        private const val MENU_ID = "ws-menu"

        private const val GLOBAL_JOURNAL = "uiserv/journal@global-journal"
        private const val URL = "/v2/journals?journalId=ws-a:not-a-ref"
    }

    @Test
    fun `artifact export replaces ws prefix with placeholder in nested items of every section`() {

        val exported = exportWsMenu()

        assertThat(exported.workspace).isEmpty()
        assertThat(refsOf(exported)).containsExactlyInAnyOrderElementsOf(
            expectedRefs("CURRENT_WS:")
        )
        assertThat(nonRefValues(exported)).containsExactlyInAnyOrder(GLOBAL_JOURNAL, URL)
        assertThat(String(exported.toBytes(), StandardCharsets.UTF_8)).doesNotContain("@$WS_A:")
    }

    @Test
    fun `artifact export reports source workspace`() {
        val exportedWs = ArrayList<String>()
        menuArtifactHandler.listenChanges { _, ws -> exportedWs.add(ws) }
        saveWsMenu()
        assertThat(exportedWs).containsExactly(WS_A)
    }

    @Test
    fun `artifact deploy into another workspace binds placeholders to it`() {

        val artifact = toArtifact(exportWsMenu())
        deploy(artifact, WS_B, listOf(EntityRef.valueOf("uiserv/journal@co-deployed")))

        val deployed = menuService.getMenu(IdInWs.create(WS_B, MENU_ID)).orElseThrow()

        assertThat(deployed.workspace).isEqualTo(WS_B)
        assertThat(refsOf(deployed)).containsExactlyInAnyOrderElementsOf(expectedRefs("$WS_B:"))
        assertThat(nonRefValues(deployed)).containsExactlyInAnyOrder(GLOBAL_JOURNAL, URL)
        assertThat(findItem(deployed, "co-deployed").config["recordRef"].asText())
            .isEqualTo("uiserv/journal@$WS_B:co-deployed")
    }

    @Test
    fun `artifact deploy into global scope gives bare refs`() {

        val artifact = toArtifact(exportWsMenu())
        deploy(artifact, "", emptyList())

        val deployed = menuService.getMenu(IdInWs.create("", MENU_ID)).orElseThrow()

        assertThat(deployed.workspace).isEmpty()
        assertThat(refsOf(deployed)).containsExactlyInAnyOrderElementsOf(expectedRefs(""))
        assertThat(nonRefValues(deployed)).containsExactlyInAnyOrder(GLOBAL_JOURNAL, URL)
    }

    @Test
    fun `artifact round trip into the same workspace restores the menu`() {

        val original = saveWsMenu()
        val artifact = toArtifact(exportWsMenu())
        deploy(artifact, WS_A, emptyList())

        val deployed = menuService.getMenu(IdInWs.create(WS_A, MENU_ID)).orElseThrow()
        assertThat(Json.mapper.toNonDefaultJson(deployed)).isEqualTo(Json.mapper.toNonDefaultJson(original))
    }

    @Test
    fun `rewrite is a no-op for a menu without ws-scoped refs`() {
        val menu = MenuDto.create()
            .withId("global")
            .withSubMenu(mapOf("left" to subMenu(item("j", "JOURNAL", "recordRef" to GLOBAL_JOURNAL))))
            .build()
        val rewritten = MenuWorkspaceRefs.rewrite(menu) {
            it.withLocalId(workspaceService.replaceWsPrefixToCurrentWsPlaceholder(it.getLocalId()))
        }
        assertThat(Json.mapper.toNonDefaultJson(rewritten)).isEqualTo(Json.mapper.toNonDefaultJson(menu))
    }

    @Test
    fun `record data export clears workspace and uses placeholders`() {

        saveWsMenu()

        val record = menuRecords.getRecordAtts("$WS_A:$MENU_ID")
        val exported = Json.mapper.convert(
            YamlUtils.read(String(record.getData(), StandardCharsets.UTF_8)),
            MenuDto::class.java
        )!!

        assertThat(exported.workspace).isEmpty()
        assertThat(refsOf(exported)).containsExactlyInAnyOrderElementsOf(expectedRefs("CURRENT_WS:"))
        assertThat(nonRefValues(exported)).containsExactlyInAnyOrder(GLOBAL_JOURNAL, URL)
        // ?json stays as is — it is not an export format
        assertThat(Json.mapper.convert(record.toJson(), MenuDto::class.java)!!.workspace).isEqualTo(WS_A)
    }

    @Test
    fun `records upload into workspace binds placeholders`() {

        saveWsMenu()
        val data = menuRecords.getRecordAtts("$WS_A:$MENU_ID").getData()
        val content = "data:application/x-yaml;base64," + Base64.getEncoder().encodeToString(data)

        val ref = AuthContext.runAsSystem {
            records.mutate(
                "menu@",
                mapOf(
                    "_content" to listOf(mapOf("url" to content)),
                    "_workspace" to "emodel/workspace@$WS_B"
                )
            )
        }
        assertThat(ref.getLocalId()).isEqualTo("$WS_B:$MENU_ID")

        val uploaded = menuService.getMenu(IdInWs.create(WS_B, MENU_ID)).orElseThrow()
        assertThat(refsOf(uploaded)).containsExactlyInAnyOrderElementsOf(expectedRefs("$WS_B:"))
        assertThat(nonRefValues(uploaded)).containsExactlyInAnyOrder(GLOBAL_JOURNAL, URL)
    }

    @Test
    fun `records upload of exported artifact json into workspace with workspace att first`() {
        uploadExportedArtifactJson(workspaceFirst = true)
    }

    @Test
    fun `records upload of exported artifact json into workspace with content att first`() {
        uploadExportedArtifactJson(workspaceFirst = false)
    }

    private fun uploadExportedArtifactJson(workspaceFirst: Boolean) {

        saveWsMenu()
        // the artifact json (as stored in ecos-apps) carries an explicit "workspace": ""
        val exported = MenuWorkspaceRefs.rewrite(menuService.getMenu(IdInWs.create(WS_A, MENU_ID)).orElseThrow()) {
            it.withLocalId(workspaceService.replaceWsPrefixToCurrentWsPlaceholder(it.getLocalId()))
        }.copy().withWorkspace("").build()
        val content = "data:application/json;base64," +
            Base64.getEncoder().encodeToString(Json.mapper.toBytes(exported))

        val contentAtt = "_content" to listOf(mapOf("url" to content))
        val wsAtt = "_workspace" to "emodel/workspace@$WS_B"
        val atts = if (workspaceFirst) linkedMapOf(wsAtt, contentAtt) else linkedMapOf(contentAtt, wsAtt)

        val ref = AuthContext.runAsSystem { records.mutate("menu@", atts) }
        assertThat(ref.getLocalId()).isEqualTo("$WS_B:$MENU_ID")

        val uploaded = menuService.getMenu(IdInWs.create(WS_B, MENU_ID)).orElseThrow()
        assertThat(refsOf(uploaded)).containsExactlyInAnyOrderElementsOf(expectedRefs("$WS_B:"))
    }

    @Test
    fun `records mutate keeps refs without placeholder as is`() {

        val menu = buildWsMenu()
        AuthContext.runAsSystem {
            records.mutate(
                "menu@",
                mapOf(
                    "id" to MENU_ID,
                    "workspace" to WS_B,
                    "subMenu" to menu.subMenu
                )
            )
        }
        val saved = menuService.getMenu(IdInWs.create(WS_B, MENU_ID)).orElseThrow()
        assertThat(refsOf(saved)).containsExactlyInAnyOrderElementsOf(expectedRefs("$WS_A:"))
    }

    @Test
    fun `workspace template export keeps ws refs as they are`() {

        // templates carry only uiserv menus and dashboards, not the journals/types/forms the refs point to,
        // so binding them to the new workspace would break the links — template menus stay unchanged
        saveWsMenu()

        val dashboardService = mock<DashboardService>()
        whenever(dashboardService.findAllForWorkspace(WS_A)).thenReturn(emptyList())
        val wsUiService = WorkspaceUiService(menuService, dashboardService, workspaceService)

        val templateDir = wsUiService.getWsArtifactsForTemplate(WS_A)
        val menuFile = templateDir.findFiles("ui/menu/**.json").single()
        val templateMenu = menuFile.read { Json.mapper.readNotNull(it, MenuDto::class.java) }!!

        assertThat(refsOf(templateMenu)).containsExactlyInAnyOrderElementsOf(expectedRefs("$WS_A:"))
    }

    private fun expectedRefs(prefix: String): List<String> {
        return listOf(
            "uiserv/journal@${prefix}journal-in-section",
            "uiserv/dashboard@${prefix}dash",
            "emodel/type@${prefix}type",
            "emodel/type@${prefix}variant-type",
            "emodel/type@${prefix}type",
            "uiserv/menu@${prefix}included",
            "emodel/doc@${prefix}record",
            "uiserv/form@${prefix}form",
            "eproc/bpmn-def@${prefix}process",
            "uiserv/journal@${prefix}deep"
        )
    }

    private fun buildWsMenu(): MenuDto {
        val left = subMenu(
            item(
                "section",
                "SECTION",
                children = listOf(
                    item("journal-in-section", "JOURNAL", "recordRef" to "uiserv/journal@$WS_A:journal-in-section"),
                    item("global-journal", "JOURNAL", "recordRef" to GLOBAL_JOURNAL),
                    item("co-deployed", "JOURNAL", "recordRef" to "uiserv/journal@co-deployed"),
                    item(
                        "sub-section",
                        "SECTION",
                        children = listOf(
                            item("deep", "KANBAN", "recordRef" to "uiserv/journal@$WS_A:deep")
                        )
                    ),
                    item("dashboard", "DASHBOARD", "dashboardId" to "uiserv/dashboard@$WS_A:dash"),
                    item("link", "ARBITRARY", "url" to URL)
                )
            )
        )
        val create = subMenu(
            item(
                "create-case",
                "LINK-CREATE-CASE",
                "typeRef" to "emodel/type@$WS_A:type",
                "variantTypeRef" to "emodel/type@$WS_A:variant-type",
                "recordRef" to "emodel/type@$WS_A:type",
                "variantId" to "default"
            )
        )
        val user = subMenu(
            item("include", "INCLUDE_MENU", "menuRef" to "uiserv/menu@$WS_A:included"),
            item(
                "edit-record",
                "EDIT_RECORD",
                "recordRef" to "emodel/doc@$WS_A:record",
                "formRef" to "uiserv/form@$WS_A:form"
            ),
            item("start-workflow", "START_WORKFLOW", "processDef" to "eproc/bpmn-def@$WS_A:process")
        )
        return MenuDto.create()
            .withId(MENU_ID)
            .withVersion(1)
            .withWorkspace(WS_A)
            .withSubMenu(mapOf("left" to left, "create" to create, "user" to user))
            .build()
    }

    private fun saveWsMenu(): MenuDto {
        return AuthContext.runAsSystem { menuService.save(buildWsMenu()) }
    }

    private fun exportWsMenu(): MenuDto {
        val artifacts = ArrayList<MenuDeployArtifact>()
        menuArtifactHandler.listenChanges { artifact, _ -> artifacts.add(artifact) }
        saveWsMenu()
        return Json.mapper.read(artifacts.last().data, MenuDto::class.java)!!
    }

    private fun toArtifact(menu: MenuDto): MenuDeployArtifact {
        val artifact = MenuDeployArtifact()
        artifact.id = menu.id
        artifact.filename = menu.id + ".json"
        artifact.data = menu.toBytes()
        return artifact
    }

    private fun MenuDto.toBytes(): ByteArray = Json.mapper.toBytesNotNull(this)

    private fun deploy(artifact: MenuDeployArtifact, workspace: String, coDeployed: List<EntityRef>) {
        val meta = ArtifactDeployMeta.create().withCoDeployedArtifacts(coDeployed).build()
        AuthContext.runAsSystem {
            ArtifactDeployMeta.doWithMeta(meta) {
                menuArtifactHandler.deployArtifact(artifact, workspace)
            }
        }
    }

    private fun subMenu(vararg items: MenuItemDef): SubMenuDef {
        val subMenu = SubMenuDef()
        subMenu.items = items.toList()
        return subMenu
    }

    private fun item(
        id: String,
        type: String,
        vararg config: Pair<String, String>,
        children: List<MenuItemDef> = emptyList()
    ): MenuItemDef {
        val cfg = ObjectData.create()
        config.forEach { (k, v) -> cfg[k] = v }
        return MenuItemDef.create()
            .withId(id)
            .withType(type)
            .withConfig(cfg)
            .withItems(children)
            .build()
    }

    private fun allItems(menu: MenuDto): List<MenuItemDef> {
        val result = ArrayList<MenuItemDef>()
        fun collect(items: List<MenuItemDef>) {
            items.forEach {
                result.add(it)
                collect(it.items)
            }
        }
        menu.subMenu.values.forEach { collect(it.items) }
        return result
    }

    private fun findItem(menu: MenuDto, id: String): MenuItemDef {
        return allItems(menu).single { it.id == id }
    }

    private fun refsOf(menu: MenuDto): List<String> {
        return allItems(menu)
            .filter { it.id != "global-journal" && it.id != "co-deployed" }
            .flatMap { item -> MenuWorkspaceRefs.REF_CONFIG_KEYS.mapNotNull { item.config[it].asText().ifEmpty { null } } }
    }

    private fun nonRefValues(menu: MenuDto): List<String> {
        val globalJournal = findItem(menu, "global-journal").config["recordRef"].asText()
        val url = findItem(menu, "link").config["url"].asText()
        return listOf(globalJournal, url)
    }
}
