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
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.dashdoard.api.records.DashboardRecords.Query
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
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

            listOf("user\$workspace", "custom-workspace", "").forEach { workspace ->

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

            val dashboardRef = EntityRef.valueOf("dashboard@" + config.id)
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

        queryDashAndExpectId(queryWithWs, "with-type-and-authority-ws-custom-workspace")
        dashboardService.removeDashboard("with-type-and-authority-ws-custom-workspace")
        queryDashAndExpectId(queryWithWs, "with-type-ws-custom-workspace")
        dashboardService.removeDashboard("with-type-ws-custom-workspace")
        queryDashAndExpectId(queryWithWs, "with-type-and-authority")
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

    private fun assertDto(expected: DashboardDto?, actual: DashboardDto?) {

        assertNotNull(expected)
        assertNotNull(actual)

        val expectedConfig = DashboardDto.Builder(expected!!)
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
            dashboardService.removeDashboard(it.id)
        }
        data.clear()
    }
}
