package ru.citeck.ecos.uiserv.domain.dashboard.api.records

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordMeta
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.RecordsService
import ru.citeck.ecos.records2.request.query.RecordsQuery
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.dashdoard.api.records.DashboardRecords
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
open class DashboardRecordsTest {

    @Autowired
    lateinit var recordsService: RecordsService

    val data = ArrayList<DashboardDto>()

    @Before
    fun prepareData() {

        recordsService.register(RecordsDaoBuilder.create("emodel/type")
            .addRecord("test-type", ObjectData.create())
            .build())

        data.clear()

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
            """
        ).forEach {

            val newDashboard = RecordMeta()
            newDashboard.setId(RecordRef.valueOf("dashboard@"))
            newDashboard.attributes = ObjectData.create(it)

            data.add(newDashboard.attributes.getAs(DashboardDto::class.java)!!)

            recordsService.mutate(newDashboard)
        }
    }

    @Test
    fun test() {

        for (config in data) {

            val dashboardRef = RecordRef.valueOf("dashboard@" + config.id)
            assertEquals(
                config.config.getData(),
                recordsService.getAtt(dashboardRef, "config?json")
            )

            val recsQuery = DashboardRecords.Query();

            if (RecordRef.isNotEmpty(config.appliedToRef)) {
                recsQuery.recordRef = config.appliedToRef
            }
            if (RecordRef.isNotEmpty(config.typeRef)) {
                recsQuery.typeRef = config.typeRef
            }
            if (!config.authority.isNullOrBlank()) {
                recsQuery.authority = config.authority
            }

            val recordsQuery = RecordsQuery()
            recordsQuery.sourceId = "dashboard"
            recordsQuery.query = recsQuery

            val queryRes = recordsService.queryRecords(recordsQuery, DashboardDto::class.java)

            assertEquals(1, queryRes.records.size)
            val dashboardRecord = queryRes.records[0]

            assertDto(config, dashboardRecord)
        }

        val recsQuery1 = DashboardRecords.Query();
        recsQuery1.authority = "admin"
        recsQuery1.typeRef = RecordRef.valueOf("emodel/type@test-type")
        recsQuery1.recordRef = RecordRef.valueOf("workspace://SpaceStore/123")

        val recordsQuery1 = RecordsQuery()
        recordsQuery1.sourceId = "dashboard"
        recordsQuery1.query = recsQuery1

        val queryRes1 = recordsService.queryRecords(recordsQuery1, DashboardDto::class.java)

        assertDto(data.firstOrNull { it.id == "with-applied-to-ref-and-authority" }, queryRes1.getRecords()[0])

        val recsQuery2 = DashboardRecords.Query();
        recsQuery2.typeRef = RecordRef.valueOf("emodel/type@test-type")
        recsQuery2.recordRef = RecordRef.valueOf("workspace://SpaceStore/123")

        val recordsQuery2 = RecordsQuery()
        recordsQuery2.sourceId = "dashboard"
        recordsQuery2.query = recsQuery2

        val queryRes2 = recordsService.queryRecords(recordsQuery2, DashboardDto::class.java)

        assertDto(data.firstOrNull { it.id == "with-applied-to-ref" }, queryRes2.getRecords()[0])

        val recsQuery3 = DashboardRecords.Query();
        recsQuery3.authority = "admin"
        recsQuery3.typeRef = RecordRef.valueOf("emodel/type@test-type")

        val recordsQuery3 = RecordsQuery()
        recordsQuery3.sourceId = "dashboard"
        recordsQuery3.query = recsQuery3

        val queryRes3 = recordsService.queryRecords(recordsQuery3, DashboardDto::class.java)

        assertDto(data.firstOrNull { it.id == "with-type-and-authority" }, queryRes3.getRecords()[0])

        val recsQuery4 = DashboardRecords.Query();
        recsQuery4.typeRef = RecordRef.valueOf("emodel/type@test-type")

        val recordsQuery4 = RecordsQuery()
        recordsQuery4.sourceId = "dashboard"
        recordsQuery4.query = recsQuery4

        val queryRes4 = recordsService.queryRecords(recordsQuery4, DashboardDto::class.java)

        assertDto(data.firstOrNull { it.id == "with-type" }, queryRes4.getRecords()[0])
    }

    private fun assertDto(expected: DashboardDto?, actual: DashboardDto?) {

        assertNotNull(expected)
        assertNotNull(actual)

        val expectedConfig = DashboardDto(expected)
        if (RecordRef.isNotEmpty(expectedConfig.appliedToRef) && expectedConfig.appliedToRef.appName.isBlank()) {
            expectedConfig.appliedToRef = expectedConfig.appliedToRef.addAppName("alfresco")
        }

        assertEquals(expectedConfig, actual)
    }
}
