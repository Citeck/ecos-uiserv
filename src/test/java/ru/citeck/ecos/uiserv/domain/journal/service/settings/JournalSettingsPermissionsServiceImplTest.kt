package ru.citeck.ecos.uiserv.domain.journal.service.settings

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier
import ru.citeck.ecos.uiserv.app.common.service.AuthoritiesSupport
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsEntity

internal class JournalSettingsPermissionsServiceImplTest {

    @Test
    fun canReadEntityUser() {
        SecurityUtils.doAsUser("user1", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "user1", "ROLE_USER"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canRead(createEntity("user1", "user1")))
                assertTrue(service.canRead(createEntity("user1", "anotherUser")))
                assertTrue(service.canRead(createEntity("anotherUser", "user1")))
                assertTrue(service.canRead(createEntity("GROUP_all", "anotherUser")))
                assertFalse(service.canRead(createEntity("anotherUser", "anotherUser")))
                assertFalse(service.canRead(createEntity("admin", "anotherUser")))
                assertFalse(service.canRead(createEntity("anotherUser", "admin")))
                assertFalse(service.canRead(createEntity("admin", "admin")))
            }
        })
    }

    @Test
    fun canReadEntityAdmin() {
        SecurityUtils.doAsUser("admin", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canRead(createEntity("user1", "user1")))
                assertTrue(service.canRead(createEntity("user1", "anotherUser")))
                assertTrue(service.canRead(createEntity("anotherUser", "user1")))
                assertTrue(service.canRead(createEntity("GROUP_all", "anotherUser")))
                assertTrue(service.canRead(createEntity("anotherUser", "anotherUser")))
                assertTrue(service.canRead(createEntity("admin", "anotherUser")))
                assertTrue(service.canRead(createEntity("anotherUser", "admin")))
                assertTrue(service.canRead(createEntity("admin", "admin")))
            }
        })
    }

    @Test
    fun canReadDtoUser() {
        SecurityUtils.doAsUser("user1", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "user1", "ROLE_USER"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("user1")
                    withCreator("user1")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("user1")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("user1")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("GROUP_all")
                    withCreator("anotherUser")
                }))
                assertFalse(service.canRead(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("anotherUser")
                }))
                assertFalse(service.canRead(JournalSettingsDto.create {
                    withAuthority("admin")
                    withCreator("anotherUser")
                }))
                assertFalse(service.canRead(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("admin")
                }))
                assertFalse(service.canRead(JournalSettingsDto.create {
                    withAuthority("admin")
                    withCreator("admin")
                }))
            }
        })
    }

    @Test
    fun canReadDtoAdmin() {
        SecurityUtils.doAsUser("admin", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("user1")
                    withCreator("user1")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("user1")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("user1")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("GROUP_all")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("admin")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("admin")
                }))
                assertTrue(service.canRead(JournalSettingsDto.create {
                    withAuthority("admin")
                    withCreator("admin")
                }))
            }
        })
    }

    @Test
    fun canWriteEntityUser() {
        SecurityUtils.doAsUser("user1", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "user1", "ROLE_USER"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canWrite(createEntity("user1", "user1")))
                assertFalse(service.canWrite(createEntity("user1", "anotherUser")))
                assertTrue(service.canWrite(createEntity("anotherUser", "user1")))
                assertFalse(service.canWrite(createEntity("GROUP_all", "anotherUser")))
                assertFalse(service.canWrite(createEntity("anotherUser", "anotherUser")))
                assertFalse(service.canWrite(createEntity("admin", "anotherUser")))
                assertFalse(service.canWrite(createEntity("anotherUser", "admin")))
                assertFalse(service.canWrite(createEntity("admin", "admin")))
            }
        })
    }

    @Test
    fun canWriteEntityAdmin() {
        SecurityUtils.doAsUser("admin", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canWrite(createEntity("user1", "user1")))
                assertTrue(service.canWrite(createEntity("user1", "anotherUser")))
                assertTrue(service.canWrite(createEntity("anotherUser", "user1")))
                assertTrue(service.canWrite(createEntity("GROUP_all", "anotherUser")))
                assertTrue(service.canWrite(createEntity("anotherUser", "anotherUser")))
                assertTrue(service.canWrite(createEntity("admin", "anotherUser")))
                assertTrue(service.canWrite(createEntity("anotherUser", "admin")))
                assertTrue(service.canWrite(createEntity("admin", "admin")))
            }
        })
    }

    @Test
    fun canWriteDtoUser() {
        SecurityUtils.doAsUser("user1", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "user1", "ROLE_USER"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("user1")
                    withCreator("user1")
                }))
                assertFalse(service.canWrite(JournalSettingsDto.create {
                    withAuthority("user1")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("user1")
                }))
                assertFalse(service.canWrite(JournalSettingsDto.create {
                    withAuthority("GROUP_all")
                    withCreator("anotherUser")
                }))
                assertFalse(service.canWrite(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("anotherUser")
                }))
                assertFalse(service.canWrite(JournalSettingsDto.create {
                    withAuthority("admin")
                    withCreator("anotherUser")
                }))
                assertFalse(service.canWrite(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("admin")
                }))
                assertFalse(service.canWrite(JournalSettingsDto.create {
                    withAuthority("admin")
                    withCreator("admin")
                }))
            }
        })
    }

    @Test
    fun canWriteDtoAdmin() {
        SecurityUtils.doAsUser("admin", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("user1")
                    withCreator("user1")
                }))
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("user1")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("user1")
                }))
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("GROUP_all")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("admin")
                    withCreator("anotherUser")
                }))
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                    withCreator("admin")
                }))
                assertTrue(service.canWrite(JournalSettingsDto.create {
                    withAuthority("admin")
                    withCreator("admin")
                }))
            }
        })
    }

    @Test
    fun canWriteNewUser() {
        SecurityUtils.doAsUser("user1", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "user1", "ROLE_USER"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canWriteNew(JournalSettingsDto.create {
                    withAuthority("user1")
                }))
                assertFalse(service.canWriteNew(JournalSettingsDto.create {
                    withAuthority("GROUP_all")
                }))
                assertFalse(service.canWriteNew(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                }))
            }
        })
    }

    @Test
    fun canWriteNewAdmin() {
        SecurityUtils.doAsUser("admin", object : UncheckedSupplier<Unit> {
            override fun get() {
                val authoritiesSupport = Mockito.mock(AuthoritiesSupport::class.java)
                Mockito.doReturn(listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN"))
                        .`when`(authoritiesSupport).currentUserAuthorities
                val service = JournalSettingsPermissionsServiceImpl(authoritiesSupport)
                assertTrue(service.canWriteNew(JournalSettingsDto.create {
                    withAuthority("user1")
                }))
                assertTrue(service.canWriteNew(JournalSettingsDto.create {
                    withAuthority("anotherUser")
                }))
                assertTrue(service.canWriteNew(JournalSettingsDto.create {
                    withAuthority("GROUP_all")
                }))
                assertTrue(service.canWriteNew(JournalSettingsDto.create {
                    withAuthority("admin")
                }))
            }
        })
    }

    private fun createEntity(authority: String, creator: String): JournalSettingsEntity {
        val result = JournalSettingsEntity()
        result.authority = authority
        result.createdBy = creator
        return result
    }
}