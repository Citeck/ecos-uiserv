package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.data.AuthState
import ru.citeck.ecos.context.lib.auth.data.SimpleAuthData
import ru.citeck.ecos.context.lib.auth.data.UndefinedAuth
import ru.citeck.ecos.context.lib.ctx.CtxScope
import ru.citeck.ecos.context.lib.ctx.EcosContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.journalsettings.api.records.JournalSettingsRecordsDao
import ru.citeck.ecos.uiserv.domain.journalsettings.dao.JournalSettingsRepoDao
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsRepository
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsPermissionsService
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsPermissionsServiceImpl
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsService
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsServiceImpl
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
internal class JournalSettingsRecordsDaoTest {

    @Autowired
    lateinit var permService: JournalSettingsPermissionsService

    @Autowired
    lateinit var repo: JournalSettingsRepository

    @Autowired
    lateinit var recordsService: RecordsService

    @Autowired
    lateinit var authoritiesApi: EcosAuthoritiesApi

    @Autowired
    lateinit var workspaceService: WorkspaceService

    @Autowired
    lateinit var jpaSearchConverterFactory: JpaSearchConverterFactory

    @Autowired
    private lateinit var ecosContext: EcosContext
    private lateinit var testScope: CtxScope

    @BeforeEach
    fun setUp() {
        repo.deleteAll()
        repo.flush()

        testScope = ecosContext.newScope()

        clearContext()
    }

    @AfterEach
    fun tearDown() {
        repo.deleteAll()
        repo.flush()
        clearContext()
    }

    @Test
    fun getId() {
        val permService = JournalSettingsPermissionsServiceImpl()
        val journalSettingsRecordsDao = JournalSettingsRepoDao(repo, jpaSearchConverterFactory)
        val dao = JournalSettingsRecordsDao(
            JournalSettingsServiceImpl(
                repo,
                permService,
                journalSettingsRecordsDao,
                workspaceService
            ),
            permService,
            authoritiesApi
        )
        assertEquals("journal-settings", dao.getId())
    }

    @Test
    fun getRecordAtts() {
        AuthContext.runAs("admin") {
            repo.save(
                JournalSettingsEntity().apply {
                    extId = "ext-id-1"
                    name = "some-name-1"
                    authorities = mutableListOf("user-1")
                    journalId = "journal-1"
                    settings = "{\"foo1\":\"bar\"}"
                }
            )
            repo.save(
                JournalSettingsEntity().apply {
                    extId = "ext-id-2"
                    name = "some-name-2"
                    authorities = mutableListOf("user-2")
                    journalId = "journal-1"
                    settings = "{\"foo2\":\"bar\"}"
                }
            )
        }

        AuthContext.runAs("user-1") {
            assertEquals("uiserv/journal-settings@ext-id-1", getAtt("ext-id-1", "?id").asText())
            assertEquals("ext-id-1", getAtt("ext-id-1", "id").asText())
            assertEquals("ext-id-1", getAtt("ext-id-1", "moduleId").asText())
            assertEquals("some-name-1", getAtt("ext-id-1", "name").asText())
            assertEquals("some-name-1", getAtt("ext-id-1", "?disp").asText())
            assertEquals("user-1", getAtt("ext-id-1", "authority").asText())
            assertEquals("journal-1", getAtt("ext-id-1", "journalId").asText())
            assertEquals("{\"foo1\":\"bar\"}", getAtt("ext-id-1", "settings").asText())
            assertEquals("admin", getAtt("ext-id-1", "creator").asText())
            assertFalse(getAtt("ext-id-1", "permissions._has.Write").asBoolean())
            assertTrue(getAtt("ext-id-1", "permissions._has.Read").asBoolean())

            assertEquals("uiserv/journal-settings@ext-id-2", getAtt("ext-id-2", "?id").asText())
            assertEquals("ext-id-2", getAtt("ext-id-2", "id").asText())
            assertEquals("ext-id-2", getAtt("ext-id-2", "moduleId").asText())
            assertEquals("", getAtt("ext-id-2", "name").asText())
            assertEquals("", getAtt("ext-id-2", "?disp").asText())
            assertEquals("", getAtt("ext-id-2", "authority").asText())
            assertEquals("", getAtt("ext-id-2", "journalId").asText())
            assertEquals("{}", getAtt("ext-id-2", "settings").asText())
            assertEquals("", getAtt("ext-id-2", "creator").asText())
            assertFalse(getAtt("ext-id-2", "permissions._has.Write").asBoolean())
            assertFalse(getAtt("ext-id-2", "permissions._has.Read").asBoolean())
        }

        AuthContext.runAs("user-2") {

            assertEquals("uiserv/journal-settings@ext-id-1", getAtt("ext-id-1", "?id").asText())
            assertEquals("ext-id-1", getAtt("ext-id-1", "id").asText())
            assertEquals("ext-id-1", getAtt("ext-id-1", "moduleId").asText())
            assertEquals("", getAtt("ext-id-1", "name").asText())
            assertEquals("", getAtt("ext-id-1", "?disp").asText())
            assertEquals("", getAtt("ext-id-1", "authority").asText())
            assertEquals("", getAtt("ext-id-1", "journalId").asText())
            assertEquals("{}", getAtt("ext-id-1", "settings").asText())
            assertEquals("", getAtt("ext-id-1", "creator").asText())
            assertFalse(getAtt("ext-id-1", "permissions._has.Write").asBoolean())
            assertFalse(getAtt("ext-id-1", "permissions._has.Read").asBoolean())

            assertEquals("uiserv/journal-settings@ext-id-2", getAtt("ext-id-2", "?id").asText())
            assertEquals("ext-id-2", getAtt("ext-id-2", "id").asText())
            assertEquals("ext-id-2", getAtt("ext-id-2", "moduleId").asText())
            assertEquals("some-name-2", getAtt("ext-id-2", "name").asText())
            assertEquals("some-name-2", getAtt("ext-id-2", "?disp").asText())
            assertEquals("user-2", getAtt("ext-id-2", "authority").asText())
            assertEquals("journal-1", getAtt("ext-id-2", "journalId").asText())
            assertEquals("{\"foo2\":\"bar\"}", getAtt("ext-id-2", "settings").asText())
            assertEquals("admin", getAtt("ext-id-2", "creator").asText())
            assertFalse(getAtt("ext-id-2", "permissions._has.Write").asBoolean())
            assertTrue(getAtt("ext-id-2", "permissions._has.Read").asBoolean())
        }

        AuthContext.runAs("admin") {

            assertEquals("uiserv/journal-settings@ext-id-1", getAtt("ext-id-1", "?id").asText())
            assertEquals("ext-id-1", getAtt("ext-id-1", "id").asText())
            assertEquals("ext-id-1", getAtt("ext-id-1", "moduleId").asText())
            assertEquals("some-name-1", getAtt("ext-id-1", "name").asText())
            assertEquals("some-name-1", getAtt("ext-id-1", "?disp").asText())
            assertEquals("user-1", getAtt("ext-id-1", "authority").asText())
            assertEquals("journal-1", getAtt("ext-id-1", "journalId").asText())
            assertEquals("{\"foo1\":\"bar\"}", getAtt("ext-id-1", "settings").asText())
            assertEquals("admin", getAtt("ext-id-1", "creator").asText())
            assertTrue(getAtt("ext-id-1", "permissions._has.Write").asBoolean())
            assertTrue(getAtt("ext-id-1", "permissions._has.Read").asBoolean())

            assertEquals("uiserv/journal-settings@ext-id-2", getAtt("ext-id-2", "?id").asText())
            assertEquals("ext-id-2", getAtt("ext-id-2", "id").asText())
            assertEquals("ext-id-2", getAtt("ext-id-2", "moduleId").asText())
            assertEquals("some-name-2", getAtt("ext-id-2", "name").asText())
            assertEquals("some-name-2", getAtt("ext-id-2", "?disp").asText())
            assertEquals("user-2", getAtt("ext-id-2", "authority").asText())
            assertEquals("journal-1", getAtt("ext-id-2", "journalId").asText())
            assertEquals("{\"foo2\":\"bar\"}", getAtt("ext-id-2", "settings").asText())
            assertEquals("admin", getAtt("ext-id-2", "creator").asText())
            assertTrue(getAtt("ext-id-2", "permissions._has.Write").asBoolean())
            assertTrue(getAtt("ext-id-2", "permissions._has.Read").asBoolean())
        }
    }

    private fun getAtt(extId: String, attName: String): DataValue {
        return recordsService.getAtt(EntityRef.create("uiserv", "journal-settings", extId), attName)
    }

    @Test
    fun getRecToMutate() {
        val journalSettingsRecordsDao = Mockito.mock(JournalSettingsRecordsDao::class.java)
        Mockito.doCallRealMethod().`when`(journalSettingsRecordsDao).getRecToMutate(any(String::class.java))
        Mockito.doAnswer {
            assertEquals("test-id", it.arguments[0])
            return@doAnswer journalSettingsRecordsDao.JournalSettingsRecord(
                EntityWithMeta(
                    JournalSettingsDto.create()
                        .withName(MLText("test-name"))
                        .build()
                )
            )
        }.`when`(journalSettingsRecordsDao).getRecordAtts(any(String::class.java))

        Mockito.verify(journalSettingsRecordsDao, Mockito.times(0)).getRecordAtts(any(String::class.java))

        val recToMutate = journalSettingsRecordsDao.getRecToMutate("test-id")
        assertEquals("{\"en\":\"test-name\"}", recToMutate.name.toString())

        Mockito.verify(journalSettingsRecordsDao, Mockito.times(1)).getRecordAtts(any(String::class.java))
    }

    @Test
    fun mutateAdminRecord() {
        // create record by admin for admin
        setContext("admin")
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", "name1")
                    .set("authority", "admin")
                    .set("journalId", "journal1")
                    .set("settings", "{}")
            )
        )
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"en\":\"name1\"}", check1?.name)
        assertEquals("admin", check1?.authorities?.firstOrNull())
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("admin", check1?.createdBy)
        clearContext()

        // try update record by user1 (check fail)
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.mutate(
                RecordAtts(
                    EntityRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                        .set("name", "user1ChangeName")
                )
            )
        }
        clearContext()

        // try update record by user2 (check fail)
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.mutate(
                RecordAtts(
                    EntityRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                        .set("name", "user2ChangeName")
                )
            )
        }
        clearContext()

        // update record by admin
        setContext("admin")
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"en\":\"name1\"}", check2?.name)
        assertEquals("admin", check2?.authorities?.firstOrNull())
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("admin", check2?.createdBy)
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", "anotherName")
            )
        )
        val check3 = repo.findByExtId("id1")
        assertNotNull(check3)
        assertEquals("id1", check3?.extId)
        assertEquals("{\"en\":\"anotherName\"}", check3?.name)
        assertEquals("admin", check3?.authorities?.firstOrNull())
        assertEquals("journal1", check3?.journalId)
        assertEquals("{}", check3?.settings)
        assertEquals("admin", check3?.createdBy)
        clearContext()
    }

    @Test
    fun mutateUserRecordCreatedByUser() {
        // create record by user1 for user1
        setContext("user1")
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", "name1")
                    .set("authority", "user1")
                    .set("journalId", "journal1")
                    .set("settings", "{}")
            )
        )
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"en\":\"name1\"}", check1?.name)
        assertEquals("user1", check1?.authorities?.firstOrNull())
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("user1", check1?.createdBy)
        clearContext()

        // try update record by user2 (check fail)
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.mutate(
                RecordAtts(
                    EntityRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                        .set("name", "user2ChangeName")
                )
            )
        }
        clearContext()

        // update record by user1
        setContext("user1")
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"en\":\"name1\"}", check2?.name)
        assertEquals("user1", check2?.authorities?.firstOrNull())
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("user1", check2?.createdBy)
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", "anotherNameUser1")
            )
        )
        val check3 = repo.findByExtId("id1")
        assertNotNull(check3)
        assertEquals("id1", check3?.extId)
        assertEquals("{\"en\":\"anotherNameUser1\"}", check3?.name)
        assertEquals("user1", check3?.authorities?.firstOrNull())
        assertEquals("journal1", check3?.journalId)
        assertEquals("{}", check3?.settings)
        assertEquals("user1", check3?.createdBy)
        clearContext()

        // update record by admin
        setContext("admin")
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", "anotherNameAdmin")
            )
        )
        val check4 = repo.findByExtId("id1")
        assertNotNull(check4)
        assertEquals("id1", check4?.extId)
        assertEquals("{\"en\":\"anotherNameAdmin\"}", check4?.name)
        assertEquals("user1", check4?.authorities?.firstOrNull())
        assertEquals("journal1", check4?.journalId)
        assertEquals("{}", check4?.settings)
        assertEquals("user1", check4?.createdBy)
        clearContext()
    }

    @Test
    fun mutateUserRecordCreatedByAdmin() {
        // create record by admin for user1
        setContext("admin")
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", "name1")
                    .set("authority", "user1")
                    .set("journalId", "journal1")
                    .set("settings", "{}")
            )
        )
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"en\":\"name1\"}", check1?.name)
        assertEquals("user1", check1?.authorities?.firstOrNull())
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("admin", check1?.createdBy)
        clearContext()

        // try update record by user1 (check fail)
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.mutate(
                RecordAtts(
                    EntityRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                        .set("name", "user1ChangeName")
                )
            )
        }
        clearContext()

        // try update record by user2 (check fail)
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.mutate(
                RecordAtts(
                    EntityRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                        .set("name", "user2ChangeName")
                )
            )
        }
        clearContext()

        // update record by admin
        setContext("admin")
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"en\":\"name1\"}", check2?.name)
        assertEquals("user1", check2?.authorities?.firstOrNull())
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("admin", check2?.createdBy)
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", "anotherNameAdmin")
            )
        )
        val check3 = repo.findByExtId("id1")
        assertNotNull(check3)
        assertEquals("id1", check3?.extId)
        assertEquals("{\"en\":\"anotherNameAdmin\"}", check3?.name)
        assertEquals("user1", check3?.authorities?.firstOrNull())
        assertEquals("journal1", check3?.journalId)
        assertEquals("{}", check3?.settings)
        assertEquals("admin", check3?.createdBy)
        clearContext()
    }

    @Test
    fun mutateGroupRecordCreatedByAdmin() {
        // create record by admin for GROUP_all
        setContext("admin")
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", "name1")
                    .set("authority", "GROUP_all")
                    .set("journalId", "journal1")
                    .set("settings", "{}")
            )
        )
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"en\":\"name1\"}", check1?.name)
        assertEquals("GROUP_all", check1?.authorities?.firstOrNull())
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("admin", check1?.createdBy)
        clearContext()

        // try update record by user1 (check fail)
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.mutate(
                RecordAtts(
                    EntityRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                        .set("name", "user1ChangeName")
                )
            )
        }
        clearContext()

        // try update record by user2 (check fail)
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.mutate(
                RecordAtts(
                    EntityRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                        .set("name", "user2ChangeName")
                )
            )
        }
        clearContext()

        // update record by admin
        setContext("admin")
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"en\":\"name1\"}", check2?.name)
        assertEquals("GROUP_all", check2?.authorities?.firstOrNull())
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("admin", check2?.createdBy)
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", "anotherNameAdmin")
            )
        )
        val check3 = repo.findByExtId("id1")
        assertNotNull(check3)
        assertEquals("id1", check3?.extId)
        assertEquals("{\"en\":\"anotherNameAdmin\"}", check3?.name)
        assertEquals("GROUP_all", check3?.authorities?.firstOrNull())
        assertEquals("journal1", check3?.journalId)
        assertEquals("{}", check3?.settings)
        assertEquals("admin", check3?.createdBy)
        clearContext()
    }

    @Test
    fun mutateMl() {
        // create record by admin for GROUP_all
        setContext("admin")
        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set(
                        "name?json",
                        ObjectData.create()
                            .set("ru", "123")
                            .set("en", "321")
                    )
                    .set("authority", "GROUP_all")
                    .set("journalId", "journal1")
                    .set("settings", "{}")
            )
        )
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"ru\":\"123\",\"en\":\"321\"}", check1?.name)
        assertEquals("GROUP_all", check1?.authorities?.firstOrNull())
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("admin", check1?.createdBy)

        recordsService.mutate(
            RecordAtts(
                EntityRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                    .set("name", DataValue.create("{\"ru\":\"some\",\"en\":\"body\"}"))
            )
        )
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"ru\":\"some\",\"en\":\"body\"}", check2?.name)
        assertEquals("GROUP_all", check2?.authorities?.firstOrNull())
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("admin", check2?.createdBy)
        clearContext()
    }

    @Test
    fun deleteAdminRecord() {
        // create record by admin
        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id1"
                name = "name1"
                authorities = mutableListOf("admin")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        // try delete record by user1
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        // try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        // delete record by admin
        setContext("admin")
        recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun deleteUserRecordCreatedByAdmin() {
        // create record by admin
        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id1"
                name = "name1"
                authorities = mutableListOf("user1")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        // try delete record by user1
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        // try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        // delete record by admin
        setContext("admin")
        recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun deleteUserRecordCreatedByUserFromUser() {
        // create record by admin
        setContext("user1")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id1"
                name = "name1"
                authorities = mutableListOf("user1")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        // try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        // delete record by user1
        setContext("user1")
        recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun deleteUserRecordCreatedByUserFromAdmin() {
        // create record by admin
        setContext("user1")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id1"
                name = "name1"
                authorities = mutableListOf("user1")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        // try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        // delete record by admin
        setContext("admin")
        recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun deleteGroupRecordCreatedByAdmin() {
        // create record by admin
        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id1"
                name = "name1"
                authorities = mutableListOf("GROUP_all")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        // try delete record by user1
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        // try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        // delete record by admin
        setContext("admin")
        recordsService.delete(EntityRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun queryRecords() {
        val journalSettingsService = Mockito.mock(JournalSettingsService::class.java)

        Mockito.doReturn(
            listOf(
                EntityWithMeta(
                    JournalSettingsDto.create()
                        .withId("id1")
                        .withName(MLText("name1"))
                        .build()
                ),
                EntityWithMeta(
                    JournalSettingsDto.create()
                        .withId("id2")
                        .withName(MLText("name2"))
                        .build()
                )
            )
        ).`when`(journalSettingsService).searchSettings(eq("journal1"), Mockito.anyList())
        Mockito.doReturn(emptyList<JournalSettingsDto>())
            .`when`(journalSettingsService).searchSettings(eq("journal2"), Mockito.anyList())

        val recordsDao = JournalSettingsRecordsDao(journalSettingsService, permService, authoritiesApi)

        Mockito.verify(journalSettingsService, Mockito.times(0)).searchSettings(any(String::class.java), Mockito.anyList())

        val queryRecords1 = recordsDao.queryRecords(
            RecordsQuery.create {
                withQuery("{\"journalId\": \"journal1\"}")
            }
        )
        assertEquals(2, queryRecords1.getTotalCount())
        assertEquals(false, queryRecords1.getHasMore())
        assertTrue(queryRecords1.getRecords().stream().anyMatch({ it.id == "id1" }))
        assertTrue(queryRecords1.getRecords().stream().anyMatch({ it.id == "id2" }))

        Mockito.verify(journalSettingsService, Mockito.times(1)).searchSettings(any(String::class.java), Mockito.anyList())

        val queryRecords2 = recordsDao.queryRecords(
            RecordsQuery.create {
                withQuery("{\"journalId\": \"journal2\"}")
            }
        )
        assertEquals(0, queryRecords2.getTotalCount())
        assertEquals(false, queryRecords2.getHasMore())

        Mockito.verify(journalSettingsService, Mockito.times(2)).searchSettings(any(String::class.java), Mockito.anyList())

        val queryRecords3 = recordsDao.queryRecords(
            RecordsQuery.create {
                withQuery("{\"journalId\": \"\"}")
            }
        )
        assertEquals(0, queryRecords3.getTotalCount())
        assertEquals(false, queryRecords3.getHasMore())

        Mockito.verify(journalSettingsService, Mockito.times(2)).searchSettings(any(String::class.java), Mockito.anyList())

        val queryRecords4 = recordsDao.queryRecords(
            RecordsQuery.create {
                withQuery("{}")
            }
        )
        assertEquals(0, queryRecords4.getTotalCount())
        assertEquals(false, queryRecords4.getHasMore())

        Mockito.verify(journalSettingsService, Mockito.times(2)).searchSettings(any(String::class.java), Mockito.anyList())
    }

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    private fun setContext(username: String) {
        val (user, authorities) = fetchUserByUsername(username)
        AuthContext.set(testScope, user.username, SimpleAuthData(user.username, authorities.map { it.authority }))
    }

    private fun fetchUserByUsername(username: String): Pair<User, List<GrantedAuthority>> {
        return if (username == "admin") {
            val authorities = listOf<GrantedAuthority>(
                SimpleGrantedAuthority("admin"),
                SimpleGrantedAuthority("GROUP_all"),
                SimpleGrantedAuthority("ROLE_ADMIN"),
                SimpleGrantedAuthority("ROLE_USER")
            )
            val user = User("admin", "admin", authorities)
            return Pair(user, authorities)
        } else if (username == "user1") {
            val authorities = listOf<GrantedAuthority>(
                SimpleGrantedAuthority("user1"),
                SimpleGrantedAuthority("GROUP_all"),
                SimpleGrantedAuthority("ROLE_USER")
            )
            val user = User("user1", "user1", authorities)
            return Pair(user, authorities)
        } else if (username == "user2") {
            val authorities = listOf<GrantedAuthority>(
                SimpleGrantedAuthority("user2"),
                SimpleGrantedAuthority("GROUP_all"),
                SimpleGrantedAuthority("ROLE_USER")
            )
            val user = User("user2", "user2", authorities)
            return Pair(user, authorities)
        } else {
            fail("unknown user: $username")
        }
    }

    private fun clearContext() {
        AuthContext.set(testScope, AuthState(UndefinedAuth, UndefinedAuth))
    }
}
