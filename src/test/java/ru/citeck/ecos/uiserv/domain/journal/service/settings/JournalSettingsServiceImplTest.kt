package ru.citeck.ecos.uiserv.domain.journal.service.settings

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.io.IOUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.file.repo.FileRepository
import ru.citeck.ecos.uiserv.domain.file.repo.FileType
import ru.citeck.ecos.uiserv.domain.file.service.FileService
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsRepository
import ru.citeck.ecos.uiserv.domain.journal.service.JournalPrefService
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
internal class JournalSettingsServiceImplTest {

    @Autowired
    lateinit var repo: JournalSettingsRepository

    @Autowired
    lateinit var permService: JournalSettingsPermissionsService

    @Autowired
    lateinit var journalPrefService: JournalPrefService

    @Autowired
    lateinit var fileService: FileService

    @Autowired
    lateinit var fileRepository: FileRepository

    @BeforeEach
    fun setUp() {
        repo.deleteAll()
        repo.flush()

        val files = fileRepository.findByType(FileType.JOURNALPREFS, PageRequest.of(0, Integer.MAX_VALUE))
        for (file in files) {
            fileService.delete(FileType.JOURNALPREFS, file.fileId)
        }

        clearContext()
    }

    @AfterEach
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
    fun getByIdTest() {
        val service = JournalSettingsServiceImpl(repo, permService, journalPrefService, fileService)

        AuthContext.runAs("user-1") {
            val journalSettingsEntity = JournalSettingsEntity()
            journalSettingsEntity.extId = "searchable-id"
            journalSettingsEntity.name = "some-name"
            journalSettingsEntity.authority = "user-1"
            journalSettingsEntity.journalId = "some-journal"
            journalSettingsEntity.settings = "{}"
            repo.save(journalSettingsEntity)
            repo.flush()

            val foundedDto = service.getById("searchable-id")
            assertNotNull(foundedDto)
            assertEquals("searchable-id", foundedDto?.id)
            assertEquals("{\"en\":\"some-name\"}", foundedDto?.name.toString())
            assertEquals("user-1", foundedDto?.authority)
            assertEquals("some-journal", foundedDto?.journalId)
            assertEquals(ObjectData.create("{}"), foundedDto?.settings)
            assertEquals("user-1", foundedDto?.creator)

            assertNull(service.getById("unknown-id"))
        }

        AuthContext.runAs("user-2") {
            assertNull(service.getById("searchable-id"))
            assertNull(service.getById("unknown-id"))
        }
    }

    @Test
    fun getByIdLegacyTest() {
        val service = JournalSettingsServiceImpl(repo, permService, journalPrefService, fileService)

        setContext("user1")
        val readFully = IOUtils.readAsBytes(
            Thread.currentThread().contextClassLoader.getResourceAsStream(
                "test/settings/JournalSettingsServiceImplTest/old-prefs.json"
            )
        )
        journalPrefService.deployOverride(
            "old-prefs-id-test-3",
            readFully,
            "user1",
            JournalPrefService.TargetType.USER,
            "journal1"
        )

        val dto = service.getById("old-prefs-id-test-3")
        assertEquals("old-prefs-id-test-3", dto?.id)
        assertEquals("{\"en\":\"old-prefs-title\"}", dto?.name.toString())
        assertEquals("user1", dto?.authority)
        assertEquals("", dto?.journalId)
        assertEquals(ObjectData.create(String(readFully)), dto?.settings)
        assertEquals("user1", dto?.creator)
        clearContext()
    }

    @Test
    fun saveTest() {
        val spyPermService = Mockito.spy(permService)
        val service = JournalSettingsServiceImpl(repo, spyPermService, journalPrefService, fileService)

        AuthContext.runAs("some-authority") {
            val createdDto = service.save(
                JournalSettingsDto.create {
                    withId("some-id")
                    withName(MLText("some-name"))
                    withAuthority("some-authority")
                    withJournalId("some-journal")
                    withSettings(ObjectData.create("{}"))
                }
            )

            assertEquals("some-id", createdDto.id)
            assertEquals("{\"en\":\"some-name\"}", createdDto.name.toString())
            assertEquals("some-authority", createdDto.authority)
            assertEquals("some-journal", createdDto.journalId)
            assertEquals(ObjectData.create("{}"), createdDto.settings)
            assertEquals("some-authority", createdDto.creator)

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))

            val updatedDto = service.save(
                JournalSettingsDto.create {
                    withId("some-id")
                    withName(MLText("updated-name"))
                    withAuthority("some-authority")
                    withJournalId("some-journal")
                    withSettings(ObjectData.create("{}"))
                }
            )

            assertEquals("some-id", updatedDto.id)
            assertEquals("{\"en\":\"updated-name\"}", updatedDto.name.toString())
            assertEquals("some-authority", updatedDto.authority)
            assertEquals("some-journal", updatedDto.journalId)
            assertEquals(ObjectData.create("{}"), updatedDto.settings)
            assertEquals("some-authority", updatedDto.creator)

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun saveByAnotherUserTest() {
        val spyPermService = Mockito.spy(JournalSettingsPermissionsServiceImpl())
        val service = JournalSettingsServiceImpl(repo, spyPermService, journalPrefService, fileService)

        AuthContext.runAs("some-authority", listOf("some-authority", "GROUP_all", "ROLE_USER")) {
            val createdDto = service.save(
                JournalSettingsDto.create {
                    withId("some-id")
                    withName(MLText("some-name"))
                    withAuthority("some-authority")
                    withJournalId("some-journal")
                    withSettings(ObjectData.create("{}"))
                }
            )

            assertEquals("some-id", createdDto.id)
            assertEquals("{\"en\":\"some-name\"}", createdDto.name.toString())
            assertEquals("some-authority", createdDto.authority)
            assertEquals("some-journal", createdDto.journalId)
            assertEquals(ObjectData.create("{}"), createdDto.settings)
            assertEquals("some-authority", createdDto.creator)

            Mockito.verify(spyPermService, Mockito.times(0)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("another-authority", listOf("another-authority", "GROUP_all", "ROLE_USER")) {
            val exception1 = assertThrows(IllegalAccessException::class.java) {
                service.save(
                    JournalSettingsDto.create {
                        withId("some-id")
                        withName(MLText("updated-name"))
                        withAuthority("some-authority")
                        withJournalId("some-journal")
                        withSettings(ObjectData.create("{}"))
                    }
                )
            }

            assertNotNull(exception1)
            assertEquals("Access denied!", exception1.message)

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(1)).canWriteNew(any(JournalSettingsDto::class.java))

            val exception2 = assertThrows(IllegalAccessException::class.java) {
                service.save(
                    JournalSettingsDto.create {
                        withId("another-id")
                        withName(MLText("another-name"))
                        withAuthority("some-authority")
                        withJournalId("some-journal")
                        withSettings(ObjectData.create("{}"))
                    }
                )
            }

            assertNotNull(exception2)
            assertEquals("Access denied!", exception2.message)

            Mockito.verify(spyPermService, Mockito.times(1)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(2)).canWriteNew(any(JournalSettingsDto::class.java))
        }

        AuthContext.runAs("admin", listOf("admin", "GROUP_all", "ROLE_USER", "ROLE_ADMIN")) {
            service.save(
                JournalSettingsDto.create {
                    withId("some-id")
                    withName(MLText("updated-name"))
                    withAuthority("some-authority")
                    withJournalId("some-journal")
                    withSettings(ObjectData.create("{}"))
                }
            )

            Mockito.verify(spyPermService, Mockito.times(2)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(2)).canWriteNew(any(JournalSettingsDto::class.java))

            val updatedDto = service.getById("some-id")
            assertNotNull(updatedDto)
            assertEquals("some-id", updatedDto?.id)
            assertEquals("{\"en\":\"updated-name\"}", updatedDto?.name.toString())
            assertEquals("some-authority", updatedDto?.authority)
            assertEquals("some-journal", updatedDto?.journalId)
            assertEquals(ObjectData.create("{}"), updatedDto?.settings)
            assertEquals("some-authority", updatedDto?.creator)

            service.save(
                JournalSettingsDto.create {
                    withId("another-id")
                    withName(MLText("another-name"))
                    withAuthority("some-authority")
                    withJournalId("some-journal")
                    withSettings(ObjectData.create("{}"))
                }
            )

            val createdForAnotherDto = service.getById("another-id")
            assertNotNull(createdForAnotherDto)
            assertEquals("another-id", createdForAnotherDto?.id)
            assertEquals("{\"en\":\"another-name\"}", createdForAnotherDto?.name.toString())
            assertEquals("some-authority", createdForAnotherDto?.authority)
            assertEquals("some-journal", createdForAnotherDto?.journalId)
            assertEquals(ObjectData.create("{}"), createdForAnotherDto?.settings)
            assertEquals("admin", createdForAnotherDto?.creator)

            Mockito.verify(spyPermService, Mockito.times(2)).canWrite(any(JournalSettingsEntity::class.java))
            Mockito.verify(spyPermService, Mockito.times(3)).canWriteNew(any(JournalSettingsDto::class.java))
        }
    }

    @Test
    fun deleteTest() {
        val spyPermService = Mockito.spy(permService)
        val service = JournalSettingsServiceImpl(repo, spyPermService, journalPrefService, fileService)

        AuthContext.runAs("some-authority") {
            service.save(
                JournalSettingsDto.create {
                    withId("some-id")
                    withName(MLText("some-name"))
                    withAuthority("some-authority")
                    withJournalId("some-journal")
                    withSettings(ObjectData.create("{}"))
                }
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
    fun deleteLegacyRecordTest() {
        val service = JournalSettingsServiceImpl(repo, permService, journalPrefService, fileService)

        setContext("user1")
        val readFully = IOUtils.readAsBytes(
            Thread.currentThread().contextClassLoader.getResourceAsStream(
                "test/settings/JournalSettingsServiceImplTest/old-prefs.json"
            )
        )
        journalPrefService.deployOverride(
            "old-prefs-id-test-4",
            readFully,
            "user1",
            JournalPrefService.TargetType.USER,
            "journal1"
        )

        assertNotNull(service.getById("old-prefs-id-test-4"))
        val dto = service.delete("old-prefs-id-test-4")
        assertNull(service.getById("old-prefs-id-test-4"))
    }

    @Test
    fun deleteByAnotherUserTest() {

        val spyPermService = Mockito.spy(JournalSettingsPermissionsServiceImpl())
        val service = JournalSettingsServiceImpl(repo, spyPermService, journalPrefService, fileService)

        AuthContext.runAs("some-authority", listOf("some-authority", "GROUP_all", "ROLE_USER")) {

            service.save(
                JournalSettingsDto.create {
                    withId("some-id")
                    withName(MLText("some-name"))
                    withAuthority("some-authority")
                    withJournalId("some-journal")
                    withSettings(ObjectData.create("{}"))
                }
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
    fun searchSettings() {
        val service = JournalSettingsServiceImpl(repo, permService, journalPrefService, fileService)

        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-1"
                name = "name-1"
                authority = "user1"
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-2"
                name = "name-2"
                authority = "user1"
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-3"
                name = "name-3"
                authority = "user2"
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-4"
                name = "name-4"
                authority = "user1"
                journalId = "journal-2"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-5"
                name = "name-5"
                authority = "GROUP_all"
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        setContext("user1")
        val foundResult1 = service.searchSettings("journal-1")
        assertEquals(3, foundResult1.size)
        assertTrue(foundResult1.stream().anyMatch({ it.id == "id-1" }))
        assertTrue(foundResult1.stream().anyMatch({ it.id == "id-2" }))
        assertTrue(foundResult1.stream().anyMatch({ it.id == "id-5" }))
        clearContext()

        setContext("user2")
        val foundResult2 = service.searchSettings("journal-1")
        assertEquals(2, foundResult2.size)
        assertTrue(foundResult2.stream().anyMatch({ it.id == "id-3" }))
        assertTrue(foundResult2.stream().anyMatch({ it.id == "id-5" }))
        clearContext()

        setContext("user1")
        val foundResult3 = service.searchSettings("journal-2")
        assertEquals(1, foundResult3.size)
        assertTrue(foundResult3.stream().anyMatch({ it.id == "id-4" }))
        clearContext()

        setContext("user2")
        val foundResult4 = service.searchSettings("journal-2")
        assertEquals(0, foundResult4.size)
        clearContext()
    }

    @Test
    fun searchUserSettingsByAdmin() {
        val service = JournalSettingsServiceImpl(repo, permService, journalPrefService, fileService)

        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-1"
                name = "name-1"
                authority = "user1"
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-2"
                name = "name-2"
                authority = "user1"
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-3"
                name = "name-3"
                authority = "user2"
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-4"
                name = "name-4"
                authority = "user1"
                journalId = "journal-2"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-5"
                name = "name-5"
                authority = "GROUP_all"
                journalId = "journal-1"
                settings = "{}"
            }
        )
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-6"
                name = "name-6"
                authority = "admin"
                journalId = "journal-1"
                settings = "{}"
            }
        )
        clearContext()

        setContext("admin")
        val foundResult1 = service.searchSettings("journal-1")
        assertEquals(5, foundResult1.size)
        assertTrue(foundResult1.stream().anyMatch { it.id == "id-1" })
        assertTrue(foundResult1.stream().anyMatch { it.id == "id-2" })
        assertTrue(foundResult1.stream().anyMatch { it.id == "id-3" })
        assertTrue(foundResult1.stream().anyMatch { it.id == "id-5" })
        assertTrue(foundResult1.stream().anyMatch { it.id == "id-6" })
        clearContext()

        setContext("admin")
        val foundResult2 = service.searchSettings("journal-2")
        assertEquals(1, foundResult2.size)
        assertTrue(foundResult2.stream().anyMatch { it.id == "id-4" })
        clearContext()

        setContext("anotherAdmin")
        val foundResult3 = service.searchSettings("journal-1")
        assertEquals(4, foundResult3.size)
        assertTrue(foundResult3.stream().anyMatch { it.id == "id-1" })
        assertTrue(foundResult3.stream().anyMatch { it.id == "id-2" })
        assertTrue(foundResult3.stream().anyMatch { it.id == "id-3" })
        assertTrue(foundResult3.stream().anyMatch { it.id == "id-5" })
        clearContext()

        setContext("anotherAdmin")
        val foundResult4 = service.searchSettings("journal-2")
        assertEquals(1, foundResult4.size)
        assertTrue(foundResult4.stream().anyMatch { it.id == "id-4" })
        clearContext()
    }

    @Test
    fun searchSettingsWithLegacyPreferences() {
        val service = JournalSettingsServiceImpl(repo, permService, journalPrefService, fileService)

        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-1"
                name = "name-1"
                authority = "GROUP_all"
                journalId = "journal1"
                settings = "{}"
            }
        )
        val readFully = IOUtils.readAsBytes(
            Thread.currentThread().contextClassLoader.getResourceAsStream(
                "test/settings/JournalSettingsServiceImplTest/old-prefs.json"
            )
        )
        journalPrefService.deployOverride(
            "old-prefs-id-test-1",
            readFully,
            "user1",
            JournalPrefService.TargetType.USER,
            "journal1"
        )
        clearContext()

        setContext("user1")
        val foundResult1 = service.searchSettings("journal1")
        assertEquals(2, foundResult1.size)
        assertEquals("old-prefs-id-test-1", foundResult1[0].id)
        assertEquals("{\"en\":\"old-prefs-title\"}", foundResult1[0].name.toString())
        assertEquals("user1", foundResult1[0].authority)
        assertEquals("journal1", foundResult1[0].journalId)
        assertEquals(ObjectData.create(String(readFully)), foundResult1[0].settings)
        assertEquals("user1", foundResult1[0].creator)
        assertEquals("id-1", foundResult1[1].id)
        assertEquals("{\"en\":\"name-1\"}", foundResult1[1].name.toString())
        assertEquals("GROUP_all", foundResult1[1].authority)
        assertEquals("journal1", foundResult1[1].journalId)
        assertEquals(ObjectData.create("{}"), foundResult1[1].settings)
        assertEquals("admin", foundResult1[1].creator)
        clearContext()

        setContext("user2")
        val foundResult2 = service.searchSettings("journal1")
        assertEquals(1, foundResult2.size)
        assertEquals("id-1", foundResult2[0].id)
        assertEquals("{\"en\":\"name-1\"}", foundResult2[0].name.toString())
        assertEquals("GROUP_all", foundResult2[0].authority)
        assertEquals("journal1", foundResult2[0].journalId)
        assertEquals(ObjectData.create("{}"), foundResult2[0].settings)
        assertEquals("admin", foundResult2[0].creator)
        clearContext()
    }

    @Test
    fun searchSettingsWithLegacyPreferencesWithCollision() {
        val service = JournalSettingsServiceImpl(repo, permService, journalPrefService, fileService)

        setContext("admin")
        repo.save(
            JournalSettingsEntity().apply {
                extId = "id-1-overriden"
                name = "name-1"
                authority = "GROUP_all"
                journalId = "journal1"
                settings = "{}"
            }
        )
        val readFully = IOUtils.readAsBytes(
            Thread.currentThread().contextClassLoader.getResourceAsStream(
                "test/settings/JournalSettingsServiceImplTest/old-prefs.json"
            )
        )
        journalPrefService.deployOverride(
            "id-1-overriden",
            readFully,
            "user1",
            JournalPrefService.TargetType.USER,
            "journal1"
        )
        clearContext()

        setContext("user1")
        val foundResult1 = service.searchSettings("journal1")
        assertEquals(1, foundResult1.size)
        assertEquals("id-1-overriden", foundResult1[0].id)
        assertEquals("{\"en\":\"name-1\"}", foundResult1[0].name.toString())
        assertEquals("GROUP_all", foundResult1[0].authority)
        assertEquals("journal1", foundResult1[0].journalId)
        assertEquals(ObjectData.create("{}"), foundResult1[0].settings)
        assertEquals("admin", foundResult1[0].creator)
        clearContext()
    }

    @Test
    fun getSettingsByAuthorityAndJournalId() {
        val service = JournalSettingsServiceImpl(repo, permService, journalPrefService, fileService)

        AuthContext.runAs("auth-1") {
            service.save(
                JournalSettingsDto.create {
                    withId("id-1")
                    withName(MLText("name-1"))
                    withAuthority("auth-1")
                    withJournalId("journal-1")
                    withSettings(ObjectData.create("{}"))
                }
            )
            service.save(
                JournalSettingsDto.create {
                    withId("id-2")
                    withName(MLText("name-2"))
                    withAuthority("auth-1")
                    withJournalId("journal-1")
                    withSettings(ObjectData.create("{}"))
                }
            )
            service.save(
                JournalSettingsDto.create {
                    withId("id-4")
                    withName(MLText("name-4"))
                    withAuthority("auth-1")
                    withJournalId("journal-2")
                    withSettings(ObjectData.create("{}"))
                }
            )
        }

        AuthContext.runAs("auth-2") {
            service.save(
                JournalSettingsDto.create {
                    withId("id-3")
                    withName(MLText("name-3"))
                    withAuthority("auth-2")
                    withJournalId("journal-1")
                    withSettings(ObjectData.create("{}"))
                }
            )
        }

        assertEquals(2, service.getSettings("auth-1", "journal-1").size)
        assertEquals(1, service.getSettings("auth-1", "journal-2").size)
        assertEquals(1, service.getSettings("auth-2", "journal-1").size)
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

    private fun clearContext() {
        SecurityContextHolder.clearContext()
    }
}
