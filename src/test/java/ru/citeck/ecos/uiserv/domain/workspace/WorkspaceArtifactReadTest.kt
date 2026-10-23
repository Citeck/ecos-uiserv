package ru.citeck.ecos.uiserv.domain.workspace

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao
import ru.citeck.ecos.uiserv.domain.board.api.records.ResolvedBoardRecordsDao
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.uiserv.domain.dashdoard.api.records.DashboardRecords
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import ru.citeck.ecos.uiserv.domain.menu.service.testutils.MenuTestBase
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.Base64
import java.util.Optional

class WorkspaceArtifactReadTest : MenuTestBase() {

    companion object {
        private const val WORKSPACE = "private-artifacts"
        private const val ARTIFACT = "private-config"
    }

    @BeforeEach
    fun setUpArtifacts() {
        workspacesApi.addMember(WORKSPACE, "member")
        workspacesApi.addMember(WORKSPACE, "manager")
        workspacesApi.addManager(WORKSPACE, "manager")

        val boardService = mock<BoardService>()
        whenever(boardService.getBoardById(any<IdInWs>())).thenAnswer {
            val id: IdInWs = it.getArgument(0)
            BoardWithMeta(BoardDef(id.id).apply { workspace = id.workspace })
        }
        val boards = BoardRecordsDao(boardService, workspaceService)
        records.register(boards)
        records.register(ResolvedBoardRecordsDao(boards, mock(), workspaceService))

        val dashboardService = mock<DashboardService>()
        whenever(dashboardService.getDashboardWithMeta(any(), any())).thenAnswer {
            Optional.of(
                EntityWithMeta(
                    DashboardDto.create()
                        .withId(it.getArgument(0))
                        .withWorkspace(it.getArgument(1))
                        .withConfig(ObjectData.create(mapOf("private" to ARTIFACT)))
                        .build()
                )
            )
        }
        records.register(DashboardRecords(dashboardService, mock(), mock(), workspaceService))

        AuthContext.runAsSystem {
            for (workspace in listOf("", WORKSPACE)) {
                val id = if (workspace.isEmpty()) {
                    "$ARTIFACT-global"
                } else {
                    ARTIFACT
                }
                menuService.save(MenuDto.create().withId(id).withWorkspace(workspace).build())
            }
        }
    }

    @Test
    fun nonmemberCannotReadWorkspaceArtifactsById() {
        AuthContext.runAs("outsider", listOf(AuthRole.USER)) {
            for (source in listOf("board", "rboard", "dashboard", "menu", "rmenu")) {
                val ref = ref(source)
                assertThat(records.getAtt(ref, "?json").toString()).doesNotContain(ARTIFACT)
                val encodedExport = records.getAtt(ref, "data?str").asText()
                val export = String(Base64.getDecoder().decode(encodedExport), Charsets.UTF_8)
                assertThat(export).doesNotContain(ARTIFACT)
                assertThat(records.getAtt(ref, "permissions._has.Write?bool").asBoolean()).isFalse()
            }
        }
    }

    @Test
    fun memberAdminAndSystemCanReadWorkspaceArtifactsById() {
        AuthContext.runAs("member", listOf(AuthRole.USER)) { assertReadable() }
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) { assertReadable() }
        AuthContext.runAsSystem { assertReadable() }
    }

    @Test
    fun menuWritePermissionMatchesSaveForMemberAndManager() {
        AuthContext.runAs("member", listOf(AuthRole.USER)) {
            assertThat(records.getAtt(ref("menu"), "permissions._has.Write?bool").asBoolean()).isFalse()
            assertThatThrownBy {
                records.mutate(ref("menu"), mapOf("type" to "changed"))
            }.hasMessageContaining("can't change menu")
        }

        AuthContext.runAs("manager", listOf(AuthRole.USER)) {
            assertThat(records.getAtt(ref("menu"), "permissions._has.Write?bool").asBoolean()).isTrue()
            records.mutate(ref("menu"), mapOf("type" to "changed"))
            assertThat(records.getAtt(ref("menu"), "type?str").asText()).isEqualTo("changed")
        }
    }

    @Test
    fun globalMenuAndDefaultTemplateKeepExistingWriteRules() {
        deployMenuArtifacts()
        AuthContext.runAs("outsider", listOf(AuthRole.USER)) {
            val global = EntityRef.create("menu", "$ARTIFACT-global")
            assertThat(records.getAtt(global, "?json").toString()).contains(ARTIFACT)
            assertThat(records.getAtt(global, "permissions._has.Write?bool").asBoolean()).isTrue()
            val templateId = MenuService.DEFAULT_MENUS.first {
                menuService.getMenu(IdInWs.create(it)).isPresent
            }
            val template = EntityRef.create("menu", templateId)
            assertThat(records.getAtt(template, "permissions._has.Write?bool").asBoolean()).isFalse()
        }
    }

    @Test
    fun boardJsonKeepsWorkspaceAndExportClearsOnlyItsCopy() {
        AuthContext.runAs("member", listOf(AuthRole.USER)) {
            val board = ref("board")
            val before = records.getAtt(board, "?json")
            assertThat(before["workspace"].asText()).isEqualTo(WORKSPACE)
            assertThat(records.getAtt(board, "workspaceRef?id").asText()).isEqualTo("emodel/workspace@$WORKSPACE")

            val encodedExport = records.getAtt(board, "data?str").asText()
            val exportedData = YamlUtils.read(String(Base64.getDecoder().decode(encodedExport), Charsets.UTF_8))
            val exported = requireNotNull(Json.mapper.convert(exportedData, BoardDef::class.java))
            assertThat(exported.id).isEqualTo(ARTIFACT)
            assertThat(exported.workspace).isNullOrEmpty()
            assertThat(records.getAtt(board, "?json")).isEqualTo(before)
        }
    }

    @Test
    fun whitespaceWorkspaceDoesNotGrantMenuWrite() {
        AuthContext.runAs("outsider", listOf(AuthRole.USER)) {
            assertThat(menuService.canWriteToWorkspace(" ")).isFalse()
            assertThatThrownBy {
                menuService.save(MenuDto.create().withId("invalid-workspace").withWorkspace(" ").build())
            }.hasMessageContaining("can't create menu")
        }
    }

    @Test
    fun globalArtifactsRemainReadable() {
        AuthContext.runAs("outsider", listOf(AuthRole.USER)) {
            for (source in listOf("board", "dashboard", "menu")) {
                val ref = EntityRef.create(source, "$ARTIFACT-global")
                assertThat(records.getAtt(ref, "?json").toString()).contains(ARTIFACT)
            }
        }
    }

    private fun assertReadable() {
        for (source in listOf("board", "rboard", "dashboard", "menu")) {
            assertThat(records.getAtt(ref(source), "?json").toString()).contains(ARTIFACT)
        }
    }

    private fun ref(source: String): EntityRef {
        return EntityRef.create(source, workspaceService.addWsPrefixToId(ARTIFACT, WORKSPACE))
    }
}
