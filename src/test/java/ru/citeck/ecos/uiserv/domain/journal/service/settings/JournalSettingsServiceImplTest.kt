package ru.citeck.ecos.uiserv.domain.journal.service.settings

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.context.lib.auth.data.AuthState
import ru.citeck.ecos.context.lib.auth.data.SimpleAuthData
import ru.citeck.ecos.context.lib.auth.data.UndefinedAuth
import ru.citeck.ecos.context.lib.ctx.CtxScope
import ru.citeck.ecos.context.lib.ctx.EcosContext
import ru.citeck.ecos.model.lib.ModelServiceFactory
import ru.citeck.ecos.model.lib.workspace.WorkspaceServiceImpl
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.journalsettings.dao.JournalSettingsDao
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsRepository
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsPermissionsService
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsService
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsServiceImpl
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
internal class JournalSettingsServiceImplTest {

    @Autowired
    lateinit var repo: JournalSettingsRepository

    @Autowired
    lateinit var permService: JournalSettingsPermissionsService

    @Autowired
    lateinit var journalSettingsService: JournalSettingsService

    @Autowired
    lateinit var journalSettingsDao: JournalSettingsDao

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

        testScope.close()

        clearContext()
    }

    @Test
    fun getByIdTest() {
        AuthContext.runAs("user-1") {
            val journalSettingsEntity = JournalSettingsEntity()
            journalSettingsEntity.extId = "searchable-id"
            journalSettingsEntity.name = "some-name"
            journalSettingsEntity.authorities = mutableListOf("user-1")
            journalSettingsEntity.setAuthoritiesForEntity(listOf("user-1", "user-3"))
            journalSettingsEntity.journalId = "some-journal"
            journalSettingsEntity.settings = "{}"
            repo.save(journalSettingsEntity)
            repo.flush()

            val foundedDto = journalSettingsService.getById("searchable-id")
            assertNotNull(foundedDto)
            assertEquals("searchable-id", foundedDto?.entity?.id)
            assertEquals("{\"en\":\"some-name\"}", foundedDto?.entity?.name.toString())
            assertEquals("user-1", foundedDto?.entity?.getAuthority())
            assertEquals(listOf("user-1", "user-3"), foundedDto?.entity?.authorities)
            assertEquals("some-journal", foundedDto?.entity?.journalId)
            assertEquals(ObjectData.create("{}"), foundedDto?.entity?.settings)
            assertEquals("user-1", foundedDto?.entity?.creator)

            assertNull(journalSettingsService.getById("unknown-id"))
        }

        AuthContext.runAs("user-2") {
            assertNull(journalSettingsService.getById("searchable-id"))
            assertNull(journalSettingsService.getById("unknown-id"))
        }

        AuthContext.runAs("user-3") {
            assertNotNull(journalSettingsService.getById("searchable-id"))
            assertNull(journalSettingsService.getById("unknown-id"))
        }
    }

    @Test
    fun saveTest() {
        val spyPermService = Mockito.spy(permService)
        val service = initSearchServiceWithSpy(spyPermService)

        AuthContext.runAs("some-authority") {
            val createdDto = service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("some-name"))
                    .withAuthority("some-authority")
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            assertEquals("some-id", createdDto.id)
            assertEquals("{\"en\":\"some-name\"}", createdDto.name.toString())
            assertEquals("some-authority", createdDto.getAuthority())
            assertEquals("some-journal", createdDto.journalId)
            assertEquals(ObjectData.create("{}"), createdDto.settings)
            assertEquals("some-authority", createdDto.creator)

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))

            val updatedDto = service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("updated-name"))
                    .withAuthority("some-authority")
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            assertEquals("some-id", updatedDto.id)
            assertEquals("{\"en\":\"updated-name\"}", updatedDto.name.toString())
            assertEquals("some-authority", updatedDto.getAuthority())
            assertEquals("some-journal", updatedDto.journalId)
            assertEquals(ObjectData.create("{}"), updatedDto.settings)
            assertEquals("some-authority", updatedDto.creator)

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun saveWithAuthoritiesTest() {
        val spyPermService = Mockito.spy(permService)
        val service = initSearchServiceWithSpy(spyPermService)

        AuthContext.runAs("some-authority-1") {
            val createdDto = service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("some-name"))
                    .withAuthorities(listOf("some-authority-1", "some-authority-2"))
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            assertEquals("some-id", createdDto.id)
            assertEquals("{\"en\":\"some-name\"}", createdDto.name.toString())
            assertEquals(listOf("some-authority-1", "some-authority-2"), createdDto.authorities)
            assertEquals("some-journal", createdDto.journalId)
            assertEquals(ObjectData.create("{}"), createdDto.settings)
            assertEquals("some-authority-1", createdDto.creator)

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))

            val updatedDto = service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("updated-name"))
                    .withAuthorities(listOf("some-authority-1", "some-authority-2"))
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            assertEquals("some-id", updatedDto.id)
            assertEquals("{\"en\":\"updated-name\"}", updatedDto.name.toString())
            assertEquals(listOf("some-authority-1", "some-authority-2"), updatedDto.authorities)
            assertEquals("some-journal", updatedDto.journalId)
            assertEquals(ObjectData.create("{}"), updatedDto.settings)
            assertEquals("some-authority-1", updatedDto.creator)

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun saveByAnotherUserTest() {
        val spyPermService = Mockito.spy(permService)
        val service = initSearchServiceWithSpy(spyPermService)

        AuthContext.runAs("some-authority", listOf("some-authority", "GROUP_all", "ROLE_USER")) {
            val createdDto = service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("some-name"))
                    .withAuthority("some-authority")
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            assertEquals("some-id", createdDto.id)
            assertEquals("{\"en\":\"some-name\"}", createdDto.name.toString())
            assertEquals("some-authority", createdDto.getAuthority())
            assertEquals("some-journal", createdDto.journalId)
            assertEquals(ObjectData.create("{}"), createdDto.settings)
            assertEquals("some-authority", createdDto.creator)

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("another-authority", listOf("another-authority", "GROUP_all", "ROLE_USER")) {
            val exception1 = assertThrows(IllegalAccessException::class.java) {
                service.save(
                    JournalSettingsDto.create()
                        .withId("some-id")
                        .withName(MLText("updated-name"))
                        .withAuthority("some-authority")
                        .withJournalId("some-journal")
                        .withSettings(ObjectData.create("{}"))
                        .build()
                )
            }

            assertNotNull(exception1)
            assertEquals("Access denied!", exception1.message)

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("admin", listOf("admin", "GROUP_all", "ROLE_USER", "ROLE_ADMIN")) {
            service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("updated-name"))
                    .withAuthority("some-authority")
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            Mockito.verify(spyPermService, Mockito.times(2)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))

            val updatedDto = service.getById("some-id")
            assertNotNull(updatedDto)
            assertEquals("some-id", updatedDto?.entity?.id)
            assertEquals("{\"en\":\"updated-name\"}", updatedDto?.entity?.name.toString())
            assertEquals("some-authority", updatedDto?.entity?.getAuthority())
            assertEquals("some-journal", updatedDto?.entity?.journalId)
            assertEquals(ObjectData.create("{}"), updatedDto?.entity?.settings)
            assertEquals("some-authority", updatedDto?.entity?.creator)

            service.save(
                JournalSettingsDto.create()
                    .withId("another-id")
                    .withName(MLText("another-name"))
                    .withAuthority("some-authority")
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            val createdForAnotherDto = service.getById("another-id")
            assertNotNull(createdForAnotherDto)
            assertEquals("another-id", createdForAnotherDto?.entity?.id)
            assertEquals("{\"en\":\"another-name\"}", createdForAnotherDto?.entity?.name.toString())
            assertEquals("some-authority", createdForAnotherDto?.entity?.getAuthority())
            assertEquals("some-journal", createdForAnotherDto?.entity?.journalId)
            assertEquals(ObjectData.create("{}"), createdForAnotherDto?.entity?.settings)
            assertEquals("admin", createdForAnotherDto?.entity?.creator)

            Mockito.verify(spyPermService, Mockito.times(2)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(2)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun saveByAnotherUserWithAuthoritiesTest() {
        val spyPermService = Mockito.spy(permService)
        val service = initSearchServiceWithSpy(spyPermService)

        AuthContext.runAs("some-authority", listOf("some-authority", "GROUP_all", "ROLE_USER")) {
            val createdDto = service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("some-name"))
                    .withAuthorities(listOf("some-authority", "some-authority-1"))
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            assertEquals("some-id", createdDto.id)
            assertEquals("{\"en\":\"some-name\"}", createdDto.name.toString())
            assertEquals(listOf("some-authority", "some-authority-1"), createdDto.authorities)
            assertEquals("some-journal", createdDto.journalId)
            assertEquals(ObjectData.create("{}"), createdDto.settings)
            assertEquals("some-authority", createdDto.creator)

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("another-authority", listOf("another-authority", "GROUP_all", "ROLE_USER")) {
            val exception1 = assertThrows(IllegalAccessException::class.java) {
                service.save(
                    JournalSettingsDto.create()
                        .withId("some-id")
                        .withName(MLText("updated-name"))
                        .withAuthorities(listOf("some-authority", "some-authority-1"))
                        .withJournalId("some-journal")
                        .withSettings(ObjectData.create("{}"))
                        .build()
                )
            }

            assertNotNull(exception1)
            assertEquals("Access denied!", exception1.message)

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("admin", listOf("admin", "GROUP_all", "ROLE_USER", "ROLE_ADMIN")) {
            service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("updated-name"))
                    .withAuthorities(listOf("some-authority", "some-authority-1"))
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            Mockito.verify(spyPermService, Mockito.times(2)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))

            val updatedDto = service.getById("some-id")
            assertNotNull(updatedDto)
            assertEquals("some-id", updatedDto?.entity?.id)
            assertEquals("{\"en\":\"updated-name\"}", updatedDto?.entity?.name.toString())
            assertEquals(listOf("some-authority", "some-authority-1"), updatedDto?.entity?.authorities)
            assertEquals("some-journal", updatedDto?.entity?.journalId)
            assertEquals(ObjectData.create("{}"), updatedDto?.entity?.settings)
            assertEquals("some-authority", updatedDto?.entity?.creator)

            service.save(
                JournalSettingsDto.create()
                    .withId("another-id")
                    .withName(MLText("another-name"))
                    .withAuthorities(listOf("some-authority", "some-authority-1"))
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            val createdForAnotherDto = service.getById("another-id")
            assertNotNull(createdForAnotherDto)
            assertEquals("another-id", createdForAnotherDto?.entity?.id)
            assertEquals("{\"en\":\"another-name\"}", createdForAnotherDto?.entity?.name.toString())
            assertEquals(listOf("some-authority", "some-authority-1"), createdForAnotherDto?.entity?.authorities)
            assertEquals("some-journal", createdForAnotherDto?.entity?.journalId)
            assertEquals(ObjectData.create("{}"), createdForAnotherDto?.entity?.settings)
            assertEquals("admin", createdForAnotherDto?.entity?.creator)

            Mockito.verify(spyPermService, Mockito.times(2)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(2)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun deleteTest() {
        val spyPermService = Mockito.spy(permService)
        val service = initSearchServiceWithSpy(spyPermService)

        AuthContext.runAs("some-authority") {
            service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("some-name"))
                    .withAuthority("some-authority")
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )
            assertNotNull(service.getById("some-id"))

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))

            assertTrue(service.delete("some-id"))
            assertNull(service.getById("some-id"))

            assertFalse(service.delete("unknown-id"))

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun deleteWihtAuthoritiesTest() {
        val spyPermService = Mockito.spy(permService)
        val service = initSearchServiceWithSpy(spyPermService)

        AuthContext.runAs("some-authority") {
            service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("some-name"))
                    .withAuthorities(listOf("some-authority", "some-authority-1"))
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )
            assertNotNull(service.getById("some-id"))

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))

            assertTrue(service.delete("some-id"))
            assertNull(service.getById("some-id"))

            assertFalse(service.delete("unknown-id"))

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun deleteByAnotherUserTest() {
        val spyPermService = Mockito.spy(permService)
        val service = initSearchServiceWithSpy(spyPermService)

        AuthContext.runAs("some-authority", listOf("some-authority", "GROUP_all", "ROLE_USER")) {

            service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("some-name"))
                    .withAuthority("some-authority")
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("another-authority", listOf("another-authority", "GROUP_all", "ROLE_USER")) {
            assertThrows(IllegalAccessException::class.java, {
                service.delete("some-id")
            })

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("admin", listOf("admin", "GROUP_all", "ROLE_USER", "ROLE_ADMIN")) {
            assertNotNull(service.getById("some-id"))
            assertTrue(service.delete("some-id"))
            assertNull(service.getById("some-id"))

            Mockito.verify(spyPermService, Mockito.times(2)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun deleteByAnotherUserWithAuthoritiesTest() {
        val spyPermService = Mockito.spy(permService)
        val service = initSearchServiceWithSpy(spyPermService)

        AuthContext.runAs("some-authority", listOf("some-authority", "GROUP_all", "ROLE_USER")) {

            service.save(
                JournalSettingsDto.create()
                    .withId("some-id")
                    .withName(MLText("some-name"))
                    .withAuthorities(listOf("some-authority", "some-authority-1"))
                    .withJournalId("some-journal")
                    .withSettings(ObjectData.create("{}"))
                    .build()
            )

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("another-authority", listOf("another-authority", "GROUP_all", "ROLE_USER")) {
            assertThrows(IllegalAccessException::class.java) {
                service.delete("some-id")
            }

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("some-authority-1", listOf("some-authority-1", "GROUP_all", "ROLE_USER")) {
            assertThrows(IllegalAccessException::class.java) {
                service.delete("some-id")
            }

            Mockito.verify(spyPermService, Mockito.times(2)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("admin", listOf("admin", "GROUP_all", "ROLE_USER", "ROLE_ADMIN")) {
            assertNotNull(service.getById("some-id"))
            assertTrue(service.delete("some-id"))
            assertNull(service.getById("some-id"))

            Mockito.verify(spyPermService, Mockito.times(3)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun searchSettings() {
        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-1"
                name = "name-1"
                authorities = mutableListOf("user1")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-2"
                name = "name-2"
                authorities = mutableListOf("user1")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-3"
                name = "name-3"
                authorities = mutableListOf("user2")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-4"
                name = "name-4"
                authorities = mutableListOf("user1")
                journalId = "journal-2"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-5"
                name = "name-5"
                authorities = mutableListOf("GROUP_all")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        setContext("user1")
        val foundResult1 = journalSettingsService.searchSettings("journal-1")
        assertEquals(3, foundResult1.size)
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-1" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-2" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-5" })
        clearContext()

        setContext("user2")
        val foundResult2 = journalSettingsService.searchSettings("journal-1")
        assertEquals(2, foundResult2.size)
        assertTrue(foundResult2.stream().anyMatch { it.entity.id == "id-3" })
        assertTrue(foundResult2.stream().anyMatch { it.entity.id == "id-5" })
        clearContext()

        setContext("user1")
        val foundResult3 = journalSettingsService.searchSettings("journal-2")
        assertEquals(1, foundResult3.size)
        assertTrue(foundResult3.stream().anyMatch({ it.entity.id == "id-4" }))
        clearContext()

        setContext("user2")
        val foundResult4 = journalSettingsService.searchSettings("journal-2")
        assertEquals(0, foundResult4.size)
        clearContext()
    }

    @Test
    fun searchSettingsWithAuthorities() {
        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-1"
                name = "name-1"
                setAuthoritiesForEntity(listOf("user1"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-2"
                name = "name-2"
                setAuthoritiesForEntity(listOf("user1"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-3"
                name = "name-3"
                setAuthoritiesForEntity(listOf("user2"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-4"
                name = "name-4"
                setAuthoritiesForEntity(listOf("user2", "user1"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-5"
                name = "name-5"
                setAuthoritiesForEntity(listOf("user1", "user2"))
                journalId = "journal-2"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-6"
                name = "name-6"
                setAuthoritiesForEntity(listOf("GROUP_all"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        setContext("user1")
        val foundResult1 = journalSettingsService.searchSettings("journal-1")
        assertEquals(4, foundResult1.size)
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-1" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-2" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-4" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-6" })
        clearContext()

        setContext("user2")
        val foundResult2 = journalSettingsService.searchSettings("journal-1")
        assertEquals(3, foundResult2.size)
        assertTrue(foundResult2.stream().anyMatch { it.entity.id == "id-3" })
        assertTrue(foundResult2.stream().anyMatch { it.entity.id == "id-4" })
        assertTrue(foundResult2.stream().anyMatch { it.entity.id == "id-6" })
        clearContext()

        setContext("user1")
        val foundResult3 = journalSettingsService.searchSettings("journal-2")
        assertEquals(1, foundResult3.size)
        assertTrue(foundResult3.stream().anyMatch { it.entity.id == "id-5" })
        clearContext()

        setContext("user2")
        val foundResult4 = journalSettingsService.searchSettings("journal-2")
        assertEquals(1, foundResult4.size)
        assertTrue(foundResult4.stream().anyMatch { it.entity.id == "id-5" })
        clearContext()
    }

    @Test
    fun searchUserSettingsByAdmin() {
        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-1"
                name = "name-1"
                authorities = mutableListOf("user1")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-2"
                name = "name-2"
                authorities = mutableListOf("user1")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-3"
                name = "name-3"
                authorities = mutableListOf("user2")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-4"
                name = "name-4"
                authorities = mutableListOf("user1")
                journalId = "journal-2"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-5"
                name = "name-5"
                authorities = mutableListOf("GROUP_all")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-6"
                name = "name-6"
                authorities = mutableListOf("admin")
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        setContext("admin")
        val foundResult1 = journalSettingsService.searchSettings("journal-1")
        assertEquals(5, foundResult1.size)
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-1" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-2" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-3" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-5" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-6" })
        clearContext()

        setContext("admin")
        val foundResult2 = journalSettingsService.searchSettings("journal-2")
        assertEquals(1, foundResult2.size)
        assertTrue(foundResult2.stream().anyMatch { it.entity.id == "id-4" })
        clearContext()

        setContext("anotherAdmin")
        val foundResult3 = journalSettingsService.searchSettings("journal-1")
        assertEquals(1, foundResult3.size)
        assertTrue(foundResult3.stream().anyMatch { it.entity.id == "id-5" })
        clearContext()

        setContext("anotherAdmin")
        val foundResult4 = journalSettingsService.searchSettings("journal-2")
        assertEquals(0, foundResult4.size)
        clearContext()
    }

    @Test
    fun searchUserSettingsByAdminWithAuthorities() {
        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-1"
                name = "name-1"
                setAuthoritiesForEntity(listOf("user1"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        setContext("anotherAdmin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-2"
                name = "name-2"
                setAuthoritiesForEntity(listOf("user1"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-3"
                name = "name-3"
                setAuthoritiesForEntity(listOf("user2"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-4"
                name = "name-4"
                setAuthoritiesForEntity(listOf("user1"))
                journalId = "journal-2"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-5"
                name = "name-5"
                setAuthoritiesForEntity(listOf("GROUP_all"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-6"
                name = "name-6"
                setAuthoritiesForEntity(listOf("admin"))
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        setContext("admin")
        val foundResult1 = journalSettingsService.searchSettings("journal-1")
        assertEquals(4, foundResult1.size)
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-1" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-3" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-5" })
        assertTrue(foundResult1.stream().anyMatch { it.entity.id == "id-6" })
        clearContext()

        setContext("admin")
        val foundResult2 = journalSettingsService.searchSettings("journal-2")
        assertEquals(1, foundResult2.size)
        assertTrue(foundResult2.stream().anyMatch { it.entity.id == "id-4" })
        clearContext()

        setContext("anotherAdmin")
        val foundResult3 = journalSettingsService.searchSettings("journal-1")
        assertEquals(2, foundResult3.size)
        assertTrue(foundResult3.stream().anyMatch { it.entity.id == "id-2" })
        assertTrue(foundResult3.stream().anyMatch { it.entity.id == "id-5" })
        clearContext()

        setContext("anotherAdmin")
        val foundResult4 = journalSettingsService.searchSettings("journal-2")
        assertEquals(0, foundResult4.size)
        clearContext()
    }

    @Test
    fun getSettingsByAuthorityAndJournalId() {
        AuthContext.runAs("auth-1") {
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-1")
                    .withName(MLText("name-1"))
                    .withAuthority("auth-1")
                    .withJournalId("journal-1")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-2")
                    .withName(MLText("name-2"))
                    .withAuthority("auth-1")
                    .withJournalId("journal-1")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-4")
                    .withName(MLText("name-4"))
                    .withAuthority("auth-1")
                    .withJournalId("journal-2")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
        }

        AuthContext.runAs("auth-2") {
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-3")
                    .withName(MLText("name-3"))
                    .withAuthority("auth-2")
                    .withJournalId("journal-1")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
        }

        assertEquals(2, journalSettingsService.getSettings("auth-1", "journal-1").size)
        assertEquals(1, journalSettingsService.getSettings("auth-1", "journal-2").size)
        assertEquals(1, journalSettingsService.getSettings("auth-2", "journal-1").size)
    }

    @Test
    fun getSettingsByAuthorityAndJournalIdWithAuthorities() {
        AuthContext.runAs("auth-1") {
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-1")
                    .withName(MLText("name-1"))
                    .withAuthorities(listOf("auth-1", "auth-2"))
                    .withJournalId("journal-1")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-2")
                    .withName(MLText("name-2"))
                    .withAuthorities(listOf("auth-1", "auth-2"))
                    .withJournalId("journal-1")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-3")
                    .withName(MLText("name-4"))
                    .withAuthorities(listOf("auth-1", "auth-2"))
                    .withJournalId("journal-2")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-4")
                    .withName(MLText("name-1"))
                    .withAuthorities(listOf("auth-1"))
                    .withJournalId("journal-1")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-5")
                    .withName(MLText("name-2"))
                    .withAuthorities(listOf("auth-1"))
                    .withJournalId("journal-1")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-6")
                    .withName(MLText("name-4"))
                    .withAuthorities(listOf("auth-1"))
                    .withJournalId("journal-2")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
        }

        AuthContext.runAs("auth-2") {
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-7")
                    .withName(MLText("name-3"))
                    .withAuthorities(listOf("auth-1", "auth-2"))
                    .withJournalId("journal-1")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
            journalSettingsService.save(
                JournalSettingsDto.create()
                    .withId("id-8")
                    .withName(MLText("name-3"))
                    .withAuthorities(listOf("auth-2"))
                    .withJournalId("journal-1")
                    .withSettings(ObjectData.create("{}"))
                    .withWorkspaces(emptyList())
                    .build()
            )
        }

        assertEquals(5, journalSettingsService.getSettings("auth-1", "journal-1").size)
        assertEquals(2, journalSettingsService.getSettings("auth-1", "journal-2").size)
        assertEquals(4, journalSettingsService.getSettings("auth-2", "journal-1").size)
        assertEquals(1, journalSettingsService.getSettings("auth-2", "journal-2").size)
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
        } else if (username == "anotherAdmin") {
            val authorities = listOf<GrantedAuthority>(
                SimpleGrantedAuthority("anotherAdmin"),
                SimpleGrantedAuthority("GROUP_all"),
                SimpleGrantedAuthority("ROLE_ADMIN"),
                SimpleGrantedAuthority("ROLE_USER")
            )
            val user = User("anotherAdmin", "anotherAdmin", authorities)
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

    private fun initSearchServiceWithSpy(spyPermService: JournalSettingsPermissionsService): JournalSettingsService {
        return JournalSettingsServiceImpl(
            repo,
            spyPermService,
            journalSettingsDao,
            WorkspaceServiceImpl(ModelServiceFactory())
        )
    }

    private fun clearContext() {
        AuthContext.set(testScope, AuthState(UndefinedAuth, UndefinedAuth))
    }

    private fun JournalSettingsService.getSettings(user: String, journalId: String): List<EntityWithMeta<JournalSettingsDto>> {
        return AuthContext.runAs(user, listOf(AuthRole.USER)) { searchSettings(journalId, emptyList()) }
    }
}
