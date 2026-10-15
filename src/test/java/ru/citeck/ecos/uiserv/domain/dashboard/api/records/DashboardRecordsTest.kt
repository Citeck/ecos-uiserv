package ru.citeck.ecos.uiserv.domain.dashboard.api.records

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.apps.app.service.LocalAppService
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.dashdoard.api.records.DashboardRecords.Query
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.Base64
import kotlin.collections.ArrayList

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class DashboardRecordsTest {

    @Autowired
    lateinit var recordsService: RecordsService
    @Autowired
    lateinit var dashboardService: DashboardService
    @Autowired
    lateinit var localAppService: LocalAppService
    @Autowired
    lateinit var workspaceService: WorkspaceService

    val data = ArrayList<DashboardDto>()

    @BeforeEach
    fun prepareData() {

        listOf(
            """
                {
                    "id": "with-type",
                    "typeRef": "emodel/type@test-type",
                    "authority": null,
                    "config": "{\"key\":\"value\"}"
                }
            """,
            """
                {
                    "id": "with-applied-to-ref",
                    "appliedToRef": "workspace://SpaceStore/123",
                    "authority": null,
                    "config": "{\"key2\":\"value2\"}"
                }
            """,
            """
                {
                    "id": "with-type-and-authority",
                    "typeRef": "emodel/type@test-type",
                    "authority": "admin",
                    "config": "{\"key4\":\"value4\"}"
                }
            """,
            """
                {
                    "id": "with-applied-to-ref-and-authority",
                    "appliedToRef": "workspace://SpaceStore/123",
                    "authority": "admin",
                    "config": "{\"key5\":\"value5\"}"
                }
            """,
            """
                {
                    "id": "with-applied-to-ref-and-authority-upper-case",
                    "appliedToRef": "workspace://SpaceStore/123",
                    "authority": "ADMIN_UPPER",
                    "config": "{\"key6\":\"value7\"}"
                }
            """
        ).forEach { dashboardDataStr ->

            val dashboardData = ObjectData.create(dashboardDataStr)

            listOf("user-workspace", "custom-workspace", "").forEach { workspace ->

                val dashboardDataWithWorkspace = if (workspace.isEmpty()) {
                    dashboardData
                } else {
                    val dataWithWs = dashboardData.deepCopy()
                    dataWithWs["workspace"] = workspace
                    dataWithWs["id"] = dataWithWs["id"].asText() + "-ws-${workspace.replace("$", "_")}"
                    dataWithWs
                }

                val newDashboard = RecordAtts()
                newDashboard.setId(EntityRef.valueOf("dashboard@"))
                newDashboard.setAttributes(dashboardDataWithWorkspace)

                data.add(newDashboard.getAtts().getAs(DashboardDto::class.java)!!)

                AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
                    recordsService.mutate(newDashboard)
                }
            }
        }
    }

    @Test
    fun test() {

        for (config in data) {

            // Workspace-scoped dashboards are addressed by a workspace-prefixed id
            // (see DashboardRecord.getRef), mirroring how the journal lists them.
            val refId = workspaceService.addWsPrefixToId(config.id, config.workspace)
            val dashboardRef = EntityRef.valueOf("dashboard@$refId")
            assertEquals(
                config.config.getData(),
                recordsService.getAtt(dashboardRef, "config?json")
            )

            val recsQuery = Query()

            if (EntityRef.isNotEmpty(config.appliedToRef)) {
                recsQuery.recordRef = config.appliedToRef
            }
            if (EntityRef.isNotEmpty(config.typeRef)) {
                recsQuery.typeRef = config.typeRef
            }
            if (config.authority.isNotBlank()) {
                recsQuery.authority = config.authority
            }
            if (config.workspace.isNotEmpty()) {
                recsQuery.workspace = config.workspace
            }

            val recordsQuery = RecordsQuery.create {
                sourceId = "dashboard"
                withQuery(recsQuery)
            }
            val queryRes = recordsService.query(recordsQuery, DashboardDto::class.java)

            assertEquals(1, queryRes.getRecords().size)
            val dashboardRecord = queryRes.getRecords()[0]

            assertDto(config, dashboardRecord)
        }

        val queryRes1 = queryDashDto {
            this.authority = "admin"
            this.typeRef = EntityRef.valueOf("emodel/type@test-type")
            this.recordRef = EntityRef.valueOf("workspace://SpaceStore/123")
        }
        assertDto(data.firstOrNull { it.id == "with-applied-to-ref-and-authority" }, queryRes1)

        val queryRes2 = queryDashDto {
            this.typeRef = EntityRef.valueOf("emodel/type@test-type")
            this.recordRef = EntityRef.valueOf("workspace://SpaceStore/123")
        }
        assertDto(data.firstOrNull { it.id == "with-applied-to-ref" }, queryRes2)

        val queryRes3 = queryDashDto {
            this.authority = "admin"
            this.typeRef = EntityRef.valueOf("emodel/type@test-type")
        }
        assertDto(data.firstOrNull { it.id == "with-type-and-authority" }, queryRes3)

        val queryRes4 = queryDashDto {
            this.typeRef = EntityRef.valueOf("emodel/type@test-type")
        }
        assertDto(data.firstOrNull { it.id == "with-type" }, queryRes4)
    }

    @Test
    fun queryWithWorkspaceTest() {

        val typeRef = EntityRef.valueOf("emodel/type@test-type")
        val authority = "admin"
        val dashboardId0 = queryDashId {
            this.typeRef = typeRef
            this.authority = authority
        }
        assertThat(dashboardId0).isEqualTo("with-type-and-authority")

        val queryWithWs = Query()
        queryWithWs.typeRef = typeRef
        queryWithWs.authority = authority
        queryWithWs.workspace = "custom-workspace"

        // Workspace dashboards are returned with a workspace-prefixed id (see DashboardRecord.getRef).
        queryDashAndExpectId(
            queryWithWs,
            workspaceService.addWsPrefixToId("with-type-and-authority-ws-custom-workspace", "custom-workspace")
        )
        dashboardService.removeDashboard("with-type-and-authority-ws-custom-workspace", "custom-workspace")
        queryDashAndExpectId(
            queryWithWs,
            workspaceService.addWsPrefixToId("with-type-ws-custom-workspace", "custom-workspace")
        )
        dashboardService.removeDashboard("with-type-ws-custom-workspace", "custom-workspace")
        // Both custom-workspace dashboards removed -> falls back to the global dashboard (no prefix).
        queryDashAndExpectId(queryWithWs, "with-type-and-authority")
    }

    @Test
    fun getDashboardByIdWithSameExtIdInMultipleWorkspacesTest() {

        // Regression for COREDEV-111: a dashboard deployed via ecos-app into several
        // workspaces produces multiple rows sharing the same extId. Lookups by id must be
        // workspace-aware, otherwise the old findByExtId(extId) threw NonUniqueResultException.
        val sharedId = "shared-multi-ws-dash"
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            listOf("", "custom-workspace", "another-workspace").forEach { ws ->
                dashboardService.saveDashboard(
                    DashboardDto.create()
                        .withId(sharedId)
                        .withWorkspace(ws)
                        .withConfig(ObjectData.create("""{"ws":"$ws"}"""))
                        .build()
                )
            }

            // Each workspace row must be independently resolvable and must not throw.
            val globalDash = dashboardService.getDashboardById(sharedId, "")
            assertThat(globalDash).isPresent
            assertThat(globalDash.get().workspace).isEqualTo("")

            val customWsDash = dashboardService.getDashboardById(sharedId, "custom-workspace")
            assertThat(customWsDash).isPresent
            assertThat(customWsDash.get().workspace).isEqualTo("custom-workspace")

            val anotherWsDash = dashboardService.getDashboardById(sharedId, "another-workspace")
            assertThat(anotherWsDash).isPresent
            assertThat(anotherWsDash.get().workspace).isEqualTo("another-workspace")
        }
    }

    @Test
    fun testWorkspaceDefaultDashboards() {

        localAppService.deployLocalArtifacts("ui/dashboard")

        queryDashAndExpectId(
            createDashQuery {
                this.typeRef = ModelUtils.getTypeRef("personal-workspace-dashboard")
            },
            "personal-ws-dashboard-default"
        )

        queryDashAndExpectId(
            createDashQuery {
                this.typeRef = ModelUtils.getTypeRef("workspace-dashboard")
            },
            "ws-dashboard-default"
        )
    }

    @Test
    fun workspaceRefReturnsCorrectEntityRef() {
        val refId = workspaceService.addWsPrefixToId("with-type-ws-custom-workspace", "custom-workspace")
        val ref = EntityRef.valueOf("dashboard@$refId")
        val wsRef = recordsService.getAtt(ref, "workspaceRef?id").asText()
        assertThat(wsRef).isEqualTo("emodel/workspace@custom-workspace")
    }

    @Test
    fun workspaceRefReturnsDefaultForGlobalDashboard() {
        val ref = EntityRef.valueOf("dashboard@with-type")
        val wsRef = recordsService.getAtt(ref, "workspaceRef?id").asText()
        assertThat(wsRef).isEqualTo("emodel/workspace@default")
    }

    @Test
    fun testOrgstructureDefaultDashboard() {

        localAppService.deployLocalArtifacts("ui/dashboard")

        val typeRef = ModelUtils.getTypeRef("orgstructure-dashboard")

        queryDashAndExpectId(
            createDashQuery {
                this.typeRef = typeRef
                this.scope = "orgstructure"
            },
            "orgstructure-dashboard"
        )

        // The dashboard belongs to the orgstructure page and is not found outside its scope
        queryDashAndExpectId(
            createDashQuery {
                this.typeRef = typeRef
                this.isExpandType = false
            },
            ""
        )
    }

    private fun queryDashDto(action: Query.() -> Unit): DashboardDto? {
        return queryDashDto(createDashQuery(action))
    }

    private fun queryDashDto(query: Query): DashboardDto? {
        return recordsService.queryOne(
            RecordsQuery.create()
                .withSourceId("dashboard")
                .withQuery(query)
                .build(),
            DashboardDto::class.java
        )
    }

    private fun queryDashId(query: Query): String {
        return recordsService.queryOne(
            RecordsQuery.create()
                .withSourceId("dashboard")
                .withQuery(query)
                .build()
        )?.getLocalId() ?: ""
    }

    private fun queryDashId(action: Query.() -> Unit): String {
        return queryDashId(createDashQuery(action))
    }

    private fun queryDashAndExpectId(query: Query, expectedId: String) {
        assertThat(queryDashId(query)).describedAs(query.toString()).isEqualTo(expectedId)
    }

    private fun createDashQuery(action: Query.() -> Unit): Query {
        val query = Query()
        action.invoke(query)
        return query
    }

    @Test
    fun `download of a workspace dashboard is json with placeholders and upload binds them`() {

        // COREDEV-580: the dashboards journal downloads ?data, which must not leak the source workspace
        val sourceWs = "custom-workspace"
        val typeRef = "emodel/type@" + workspaceService.addWsPrefixToId("ws-type", sourceWs)
        AuthContext.runAsSystem {
            dashboardService.saveDashboard(
                DashboardDto.create()
                    .withId("downloaded")
                    .withConfig(ObjectData.create("""{"v2":{"widgets":[{"props":{"config":{"v2":{"journalId":"$sourceWs:nested-journal"}}}}]}}"""))
                    .withWorkspace(sourceWs)
                    .withTypeRef(EntityRef.valueOf(typeRef))
                    .build()
            )
        }

        val ref = EntityRef.valueOf("dashboard@" + workspaceService.addWsPrefixToId("downloaded", sourceWs))
        val data = AuthContext.runAsSystem { recordsService.getAtt(ref, "data").asText() }
        val exported = Json.mapper.readNotNull(Base64.getDecoder().decode(data), ObjectData::class.java)

        assertThat(exported["workspace"].asText()).isEmpty()
        assertThat(exported["config"].toString()).contains("CURRENT_WS:nested-journal")
        assertThat(exported["typeRef"].asText()).isEqualTo("emodel/type@CURRENT_WS:ws-type")

        // the module upload form of a workspace journal sends the file in _content and the journal
        // workspace in _workspace; the attribute order must not matter.
        // A type has one dashboard per workspace, so each order goes to its own workspace
        for ((idx, contentFirst) in listOf(true, false).withIndex()) {
            val targetWs = "another-workspace-$idx"
            exported["id"] = "uploaded-$idx"
            val content = "data:application/json;base64," +
                Base64.getEncoder().encodeToString(Json.mapper.toBytesNotNull(exported))
            val contentAtt = "_content" to listOf(mapOf("url" to content))
            val wsAtt = "_workspace" to "emodel/workspace@$targetWs"
            val atts = ObjectData.create(
                if (contentFirst) linkedMapOf(contentAtt, wsAtt) else linkedMapOf(wsAtt, contentAtt)
            )
            AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
                recordsService.mutate(RecordAtts(EntityRef.valueOf("dashboard@"), atts))
            }

            val uploaded = dashboardService.getDashboardById("uploaded-$idx", targetWs).orElseThrow()
            assertThat(uploaded.config.toString()).contains("$targetWs:nested-journal")
            assertThat(uploaded.typeRef.getLocalId())
                .isEqualTo(workspaceService.addWsPrefixToId("ws-type", targetWs))
            assertThat(dashboardService.getDashboardById("uploaded-$idx", "")).isEmpty()
        }
    }

    private fun assertDto(expected: DashboardDto?, actual: DashboardDto?) {

        assertNotNull(expected)
        assertNotNull(actual)

        val expectedConfig = DashboardDto.Builder(expected!!)
        // Queried records expose a workspace-prefixed id (see DashboardRecord.getRef);
        // global dashboards keep a plain id.
        expectedConfig.withId(workspaceService.addWsPrefixToId(expectedConfig.id, expectedConfig.workspace))
        if (EntityRef.isNotEmpty(expectedConfig.appliedToRef) && expectedConfig.appliedToRef.getAppName().isBlank()) {
            expectedConfig.withAppliedToRef(expectedConfig.appliedToRef.withAppName("alfresco"))
        }
        if (expectedConfig.authority.isNotBlank()) {
            expectedConfig.withAuthority(expectedConfig.authority.lowercase())
        }

        assertEquals(expectedConfig.build(), actual)
    }

    @AfterEach
    fun afterEach() {
        dashboardService.getAllDashboards().forEach {
            dashboardService.removeDashboard(it.id, it.workspace)
        }
        data.clear()
    }
}
