package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.core.userdetails.User
import org.springframework.test.context.junit4.SpringRunner
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.app.common.service.AuthoritiesSupport
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils
import ru.citeck.ecos.uiserv.domain.file.repo.FileRepository
import ru.citeck.ecos.uiserv.domain.file.repo.FileType
import ru.citeck.ecos.uiserv.domain.file.service.FileService
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsRepository
import ru.citeck.ecos.uiserv.domain.journal.service.JournalPrefService
import ru.citeck.ecos.uiserv.domain.journal.service.settings.JournalSettingsPermissionsService
import ru.citeck.ecos.uiserv.domain.journal.service.settings.JournalSettingsPermissionsServiceImpl
import ru.citeck.ecos.uiserv.domain.journal.service.settings.JournalSettingsService
import ru.citeck.ecos.uiserv.domain.journal.service.settings.JournalSettingsServiceImpl

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
internal class JournalSettingsRecordsDaoTest {

    @Autowired
    lateinit var authoritiesSupport: AuthoritiesSupport

    @Autowired
    lateinit var permService: JournalSettingsPermissionsService

    @Autowired
    lateinit var repo: JournalSettingsRepository

    @Autowired
    lateinit var recordsService: RecordsService

    @Autowired
    lateinit var journalPrefService: JournalPrefService

    @Autowired
    lateinit var fileService: FileService

    @Autowired
    lateinit var fileRepository: FileRepository

    @Before
    fun setUp() {
        repo.deleteAll()
        repo.flush()

        val files = fileRepository.findByType(FileType.JOURNALPREFS, PageRequest.of(0, Integer.MAX_VALUE))
        for (file in files) {
            fileService.delete(FileType.JOURNALPREFS, file.fileId)
        }

        clearContext()
    }

    @After
    fun tearDown() {
        repo.deleteAll()
        repo.flush()

        val files = fileRepository.findByType(FileType.JOURNALPREFS, PageRequest.of(0, Integer.MAX_VALUE))
        for (file in files) {
            fileService.delete(FileType.JOURNALPREFS, file.fileId)
        }

        clearContext()
    }

    @Test
    fun getId() {
        val permService = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
        val dao = JournalSettingsRecordsDao(
                JournalSettingsServiceImpl(authoritiesSupport, repo, permService, journalPrefService, fileService),
                permService)
        assertEquals("journal-settings", dao.getId())
    }

    @Test
    fun getRecordAtts() {
        SecurityUtils.doAsUser("admin", object : UncheckedSupplier<Unit> {
            override fun get() {
                repo.save(JournalSettingsEntity().apply {
                    extId = "ext-id-1"
                    name = "some-name-1"
                    authority = "user-1"
                    journalId = "journal-1"
                    settings = "{\"foo1\":\"bar\"}"
                })
                repo.save(JournalSettingsEntity().apply {
                    extId = "ext-id-2"
                    name = "some-name-2"
                    authority = "user-2"
                    journalId = "journal-1"
                    settings = "{\"foo2\":\"bar\"}"
                })
            }
        })

        SecurityUtils.doAsUser("user-1", object : UncheckedSupplier<Unit> {
            override fun get() {
                assertEquals("journal-settings@ext-id-1", getAtt("ext-id-1", "?id").asText())
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

                assertEquals("journal-settings@ext-id-2", getAtt("ext-id-2", "?id").asText())
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
        })

        SecurityUtils.doAsUser("user-2", object : UncheckedSupplier<Unit> {
            override fun get() {
                assertEquals("journal-settings@ext-id-1", getAtt("ext-id-1", "?id").asText())
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

                assertEquals("journal-settings@ext-id-2", getAtt("ext-id-2", "?id").asText())
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
        })

        SecurityUtils.doAsUser("admin", object : UncheckedSupplier<Unit> {
            override fun get() {
                assertEquals("journal-settings@ext-id-1", getAtt("ext-id-1", "?id").asText())
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

                assertEquals("journal-settings@ext-id-2", getAtt("ext-id-2", "?id").asText())
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
        })
    }

    private fun getAtt(extId: String, attName: String): DataValue {
        return recordsService.getAtt(RecordRef.create("uiserv", "journal-settings", extId), attName)
    }

    @Test
    fun getRecToMutate() {
        val journalSettingsRecordsDao = Mockito.mock(JournalSettingsRecordsDao::class.java)
        Mockito.doCallRealMethod().`when`(journalSettingsRecordsDao).getRecToMutate(any(String::class.java))
        Mockito.doAnswer {
            assertEquals("test-id", it.arguments[0])
            return@doAnswer journalSettingsRecordsDao.JournalSettingsRecord(JournalSettingsDto.create {
                withName(MLText("test-name"))
            })
        }.`when`(journalSettingsRecordsDao).getRecordAtts(any(String::class.java))

        Mockito.verify(journalSettingsRecordsDao, Mockito.times(0)).getRecordAtts(any(String::class.java))

        val recToMutate = journalSettingsRecordsDao.getRecToMutate("test-id")
        assertEquals("{\"en\":\"test-name\"}", recToMutate.name.toString())

        Mockito.verify(journalSettingsRecordsDao, Mockito.times(1)).getRecordAtts(any(String::class.java))
    }

    @Test
    fun mutateAdminRecord() {
        //create record by admin for admin
        setContext("admin")
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "name1")
                        .set("authority", "admin")
                        .set("journalId", "journal1")
                        .set("settings", "{}")
        ))
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"en\":\"name1\"}", check1?.name)
        assertEquals("admin", check1?.authority)
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("admin", check1?.createdBy)
        clearContext()

        //try update record by user1 (check fail)
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.mutate(RecordAtts(
                    RecordRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                            .set("name", "user1ChangeName")
            ))
        }
        clearContext()

        //try update record by user2 (check fail)
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.mutate(RecordAtts(
                    RecordRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                            .set("name", "user2ChangeName")
            ))
        }
        clearContext()

        //update record by admin
        setContext("admin")
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"en\":\"name1\"}", check2?.name)
        assertEquals("admin", check2?.authority)
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("admin", check2?.createdBy)
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "anotherName")
        ))
        val check3 = repo.findByExtId("id1")
        assertNotNull(check3)
        assertEquals("id1", check3?.extId)
        assertEquals("{\"en\":\"anotherName\"}", check3?.name)
        assertEquals("admin", check3?.authority)
        assertEquals("journal1", check3?.journalId)
        assertEquals("{}", check3?.settings)
        assertEquals("admin", check3?.createdBy)
        clearContext()
    }

    @Test
    fun mutateUserRecordCreatedByUser() {
        //create record by user1 for user1
        setContext("user1")
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "name1")
                        .set("authority", "user1")
                        .set("journalId", "journal1")
                        .set("settings", "{}")
        ))
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"en\":\"name1\"}", check1?.name)
        assertEquals("user1", check1?.authority)
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("user1", check1?.createdBy)
        clearContext()

        //try update record by user2 (check fail)
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.mutate(RecordAtts(
                    RecordRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                            .set("name", "user2ChangeName")
            ))
        }
        clearContext()

        //update record by user1
        setContext("user1")
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"en\":\"name1\"}", check2?.name)
        assertEquals("user1", check2?.authority)
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("user1", check2?.createdBy)
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "anotherNameUser1")
        ))
        val check3 = repo.findByExtId("id1")
        assertNotNull(check3)
        assertEquals("id1", check3?.extId)
        assertEquals("{\"en\":\"anotherNameUser1\"}", check3?.name)
        assertEquals("user1", check3?.authority)
        assertEquals("journal1", check3?.journalId)
        assertEquals("{}", check3?.settings)
        assertEquals("user1", check3?.createdBy)
        clearContext()

        //update record by admin
        setContext("admin")
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "anotherNameAdmin")
        ))
        val check4 = repo.findByExtId("id1")
        assertNotNull(check4)
        assertEquals("id1", check4?.extId)
        assertEquals("{\"en\":\"anotherNameAdmin\"}", check4?.name)
        assertEquals("user1", check4?.authority)
        assertEquals("journal1", check4?.journalId)
        assertEquals("{}", check4?.settings)
        assertEquals("user1", check4?.createdBy)
        clearContext()
    }

    @Test
    fun mutateUserRecordCreatedByAdmin() {
        //create record by admin for user1
        setContext("admin")
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "name1")
                        .set("authority", "user1")
                        .set("journalId", "journal1")
                        .set("settings", "{}")
        ))
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"en\":\"name1\"}", check1?.name)
        assertEquals("user1", check1?.authority)
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("admin", check1?.createdBy)
        clearContext()

        //try update record by user1 (check fail)
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.mutate(RecordAtts(
                    RecordRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                            .set("name", "user1ChangeName")
            ))
        }
        clearContext()

        //try update record by user2 (check fail)
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.mutate(RecordAtts(
                    RecordRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                            .set("name", "user2ChangeName")
            ))
        }
        clearContext()

        //update record by admin
        setContext("admin")
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"en\":\"name1\"}", check2?.name)
        assertEquals("user1", check2?.authority)
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("admin", check2?.createdBy)
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "anotherNameAdmin")
        ))
        val check3 = repo.findByExtId("id1")
        assertNotNull(check3)
        assertEquals("id1", check3?.extId)
        assertEquals("{\"en\":\"anotherNameAdmin\"}", check3?.name)
        assertEquals("user1", check3?.authority)
        assertEquals("journal1", check3?.journalId)
        assertEquals("{}", check3?.settings)
        assertEquals("admin", check3?.createdBy)
        clearContext()
    }

    @Test
    fun mutateGroupRecordCreatedByAdmin() {
        //create record by admin for GROUP_all
        setContext("admin")
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "name1")
                        .set("authority", "GROUP_all")
                        .set("journalId", "journal1")
                        .set("settings", "{}")
        ))
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"en\":\"name1\"}", check1?.name)
        assertEquals("GROUP_all", check1?.authority)
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("admin", check1?.createdBy)
        clearContext()

        //try update record by user1 (check fail)
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.mutate(RecordAtts(
                    RecordRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                            .set("name", "user1ChangeName")
            ))
        }
        clearContext()

        //try update record by user2 (check fail)
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.mutate(RecordAtts(
                    RecordRef.create("uiserv", "journal-settings", "id1"),
                    ObjectData.create()
                            .set("name", "user2ChangeName")
            ))
        }
        clearContext()

        //update record by admin
        setContext("admin")
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"en\":\"name1\"}", check2?.name)
        assertEquals("GROUP_all", check2?.authority)
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("admin", check2?.createdBy)
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "anotherNameAdmin")
        ))
        val check3 = repo.findByExtId("id1")
        assertNotNull(check3)
        assertEquals("id1", check3?.extId)
        assertEquals("{\"en\":\"anotherNameAdmin\"}", check3?.name)
        assertEquals("GROUP_all", check3?.authority)
        assertEquals("journal1", check3?.journalId)
        assertEquals("{}", check3?.settings)
        assertEquals("admin", check3?.createdBy)
        clearContext()
    }

    @Test
    fun mutateMl() {
        //create record by admin for GROUP_all
        setContext("admin")
        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name?json", ObjectData.create()
                                .set("ru", "123")
                                .set("en", "321"))
                        .set("authority", "GROUP_all")
                        .set("journalId", "journal1")
                        .set("settings", "{}")
        ))
        val check1 = repo.findByExtId("id1")
        assertNotNull(check1)
        assertEquals("id1", check1?.extId)
        assertEquals("{\"ru\":\"123\",\"en\":\"321\"}", check1?.name)
        assertEquals("GROUP_all", check1?.authority)
        assertEquals("journal1", check1?.journalId)
        assertEquals("{}", check1?.settings)
        assertEquals("admin", check1?.createdBy)

        recordsService.mutate(RecordAtts(
                RecordRef.create("uiserv", "journal-settings", "id1"),
                ObjectData.create()
                        .set("name", "{\"ru\":\"some\",\"en\":\"body\"}")
        ))
        val check2 = repo.findByExtId("id1")
        assertNotNull(check2)
        assertEquals("id1", check2?.extId)
        assertEquals("{\"ru\":\"some\",\"en\":\"body\"}", check2?.name)
        assertEquals("GROUP_all", check2?.authority)
        assertEquals("journal1", check2?.journalId)
        assertEquals("{}", check2?.settings)
        assertEquals("admin", check2?.createdBy)
        clearContext()
    }

    @Test
    fun deleteAdminRecord() {
        //create record by admin
        setContext("admin")
        repo.save(JournalSettingsEntity().apply {
            extId = "id1"
            name = "name1"
            authority = "admin"
            journalId = "journal-1"
            settings = "{}"
        })
        clearContext()

        //try delete record by user1
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        //try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        //delete record by admin
        setContext("admin")
        recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun deleteUserRecordCreatedByAdmin() {
        //create record by admin
        setContext("admin")
        repo.save(JournalSettingsEntity().apply {
            extId = "id1"
            name = "name1"
            authority = "user1"
            journalId = "journal-1"
            settings = "{}"
        })
        clearContext()

        //try delete record by user1
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        //try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        //delete record by admin
        setContext("admin")
        recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun deleteUserRecordCreatedByUserFromUser() {
        //create record by admin
        setContext("user1")
        repo.save(JournalSettingsEntity().apply {
            extId = "id1"
            name = "name1"
            authority = "user1"
            journalId = "journal-1"
            settings = "{}"
        })
        clearContext()

        //try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        //delete record by user1
        setContext("user1")
        recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun deleteUserRecordCreatedByUserFromAdmin() {
        //create record by admin
        setContext("user1")
        repo.save(JournalSettingsEntity().apply {
            extId = "id1"
            name = "name1"
            authority = "user1"
            journalId = "journal-1"
            settings = "{}"
        })
        clearContext()

        //try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        //delete record by admin
        setContext("admin")
        recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun deleteGroupRecordCreatedByAdmin() {
        //create record by admin
        setContext("admin")
        repo.save(JournalSettingsEntity().apply {
            extId = "id1"
            name = "name1"
            authority = "GROUP_all"
            journalId = "journal-1"
            settings = "{}"
        })
        clearContext()

        //try delete record by user1
        setContext("user1")
        assertThrows(Exception::class.java) {
            recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        //try delete record by user2
        setContext("user2")
        assertThrows(Exception::class.java) {
            recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        }
        clearContext()

        assertNotNull(repo.findByExtId("id1"))

        //delete record by admin
        setContext("admin")
        recordsService.delete(RecordRef.create("uiserv", "journal-settings", "id1"))
        clearContext()

        assertNull(repo.findByExtId("id1"))
    }

    @Test
    fun queryRecords() {
        val journalSettingsService = Mockito.mock(JournalSettingsService::class.java)

        Mockito.doReturn(listOf(
                JournalSettingsDto.create {
                    withId("id1")
                    withName(MLText("name1"))
                },
                JournalSettingsDto.create {
                    withId("id2")
                    withName(MLText("name2"))
                }
        )).`when`(journalSettingsService).searchSettings("journal1")
        Mockito.doReturn(emptyList<JournalSettingsDto>())
                .`when`(journalSettingsService).searchSettings("journal2")

        val recordsDao = JournalSettingsRecordsDao(journalSettingsService, permService)

        Mockito.verify(journalSettingsService, Mockito.times(0)).searchSettings(any(String::class.java))

        val queryRecords1 = recordsDao.queryRecords(RecordsQuery.create {
            withQuery("{\"journalId\": \"journal1\"}")
        })
        assertEquals(2, queryRecords1.getTotalCount())
        assertEquals(false, queryRecords1.getHasMore())
        assertTrue(queryRecords1.getRecords().stream().anyMatch({ it.id == "id1" }))
        assertTrue(queryRecords1.getRecords().stream().anyMatch({ it.id == "id2" }))

        Mockito.verify(journalSettingsService, Mockito.times(1)).searchSettings(any(String::class.java))

        val queryRecords2 = recordsDao.queryRecords(RecordsQuery.create {
            withQuery("{\"journalId\": \"journal2\"}")
        })
        assertEquals(0, queryRecords2.getTotalCount())
        assertEquals(false, queryRecords2.getHasMore())

        Mockito.verify(journalSettingsService, Mockito.times(2)).searchSettings(any(String::class.java))

        val queryRecords3 = recordsDao.queryRecords(RecordsQuery.create {
            withQuery("{\"journalId\": \"\"}")
        })
        assertEquals(0, queryRecords3.getTotalCount())
        assertEquals(false, queryRecords3.getHasMore())

        Mockito.verify(journalSettingsService, Mockito.times(2)).searchSettings(any(String::class.java))

        val queryRecords4 = recordsDao.queryRecords(RecordsQuery.create {
            withQuery("{}")
        })
        assertEquals(0, queryRecords4.getTotalCount())
        assertEquals(false, queryRecords4.getHasMore())

        Mockito.verify(journalSettingsService, Mockito.times(2)).searchSettings(any(String::class.java))
    }

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    private fun setContext(username: String) {
        val (user, authorities) = fetchUserByUsername(username)
        SecurityContextHolder.setContext(SecurityContextImpl(TestingAuthenticationToken(user, "creds", authorities)))
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
        SecurityContextHolder.clearContext()
    }
}
