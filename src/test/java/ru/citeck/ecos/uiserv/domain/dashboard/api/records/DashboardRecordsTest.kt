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
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils
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
        recordsService.register(RecordsDaoBuilder.create("alfresco/people")
            .addRecord("admin", ObjectData.create("{\"isAdmin\":true}"))
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

            val newDashboard = RecordAtts()
            newDashboard.setId(RecordRef.valueOf("dashboard@"))
            newDashboard.setAttributes(ObjectData.create(it))

            data.add(newDashboard.getAtts().getAs(DashboardDto::class.java)!!)

            SecurityUtils.doAsUser("admin", object : UncheckedSupplier<RecordRef> {
                override fun get(): RecordRef {
                    return recordsService.mutate(newDashboard)
                }
            })
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

            val recordsQuery = RecordsQuery.create {
                sourceId = "dashboard"
                withQuery(recsQuery)
            }
            val queryRes = recordsService.query(recordsQuery, DashboardDto::class.java)

            assertEquals(1, queryRes.getRecords().size)
            val dashboardRecord = queryRes.getRecords()[0]

            assertDto(config, dashboardRecord)
        }

        val recsQuery1 = DashboardRecords.Query();
        recsQuery1.authority = "admin"
        recsQuery1.typeRef = RecordRef.valueOf("emodel/type@test-type")
        recsQuery1.recordRef = RecordRef.valueOf("workspace://SpaceStore/123")

        val recordsQuery1 = RecordsQuery.create {
            sourceId = "dashboard"
            withQuery(recsQuery1)
        }

        val queryRes1 = recordsService.query(recordsQuery1, DashboardDto::class.java)

        assertDto(data.firstOrNull { it.id == "with-applied-to-ref-and-authority" }, queryRes1.getRecords()[0])

        val recsQuery2 = DashboardRecords.Query();
        recsQuery2.typeRef = RecordRef.valueOf("emodel/type@test-type")
        recsQuery2.recordRef = RecordRef.valueOf("workspace://SpaceStore/123")

        val recordsQuery2 = RecordsQuery.create {
            sourceId = "dashboard"
            withQuery(recsQuery2)
        }

        val queryRes2 = recordsService.query(recordsQuery2, DashboardDto::class.java)

        assertDto(data.firstOrNull { it.id == "with-applied-to-ref" }, queryRes2.getRecords()[0])

        val recsQuery3 = DashboardRecords.Query();
        recsQuery3.authority = "admin"
        recsQuery3.typeRef = RecordRef.valueOf("emodel/type@test-type")

        val recordsQuery3 = RecordsQuery.create {
            sourceId = "dashboard"
            withQuery(recsQuery3)
        }

        val queryRes3 = recordsService.query(recordsQuery3, DashboardDto::class.java)

        assertDto(data.firstOrNull { it.id == "with-type-and-authority" }, queryRes3.getRecords()[0])

        val recsQuery4 = DashboardRecords.Query();
        recsQuery4.typeRef = RecordRef.valueOf("emodel/type@test-type")

        val recordsQuery4 = RecordsQuery.create {
            sourceId = "dashboard"
            withQuery(recsQuery4)
        }

        val queryRes4 = recordsService.query(recordsQuery4, DashboardDto::class.java)

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
