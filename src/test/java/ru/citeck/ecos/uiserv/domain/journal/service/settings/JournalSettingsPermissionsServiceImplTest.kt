package ru.citeck.ecos.uiserv.domain.journal.service.settings

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsPermissionsServiceImpl

internal class JournalSettingsPermissionsServiceImplTest {

    @Test
    fun canReadEntityUser() {
        AuthContext.runAs("user1", listOf("GROUP_all", "user1", "ROLE_USER")) {
            val service = JournalSettingsPermissionsServiceImpl()

            assertTrue(service.canRead(createEntity("user1", "user1")))
            assertTrue(service.canRead(createEntity("user1", "anotherUser")))
            assertTrue(service.canRead(createEntity("anotherUser", "user1")))
            assertTrue(service.canRead(createEntity("GROUP_all", "anotherUser")))
            assertFalse(service.canRead(createEntity("anotherUser", "anotherUser")))
            assertFalse(service.canRead(createEntity("admin", "anotherUser")))
            assertFalse(service.canRead(createEntity("anotherUser", "admin")))
            assertFalse(service.canRead(createEntity("admin", "admin")))


            assertTrue(service.canRead(createEntity("user1", listOf("user1", "anotherUser"), "user1")))
            assertTrue(service.canRead(createEntity("", listOf("user1"), "anotherUser")))
            assertTrue(service.canRead(createEntity("anotherUser", listOf("anotherUser", "GROUP_all"), "user1")))
            assertTrue(service.canRead(createEntity("", listOf("GROUP_all", "admin"), "anotherUser")))

            assertFalse(service.canRead(createEntity("anotherUser", listOf("anotherUser"),"anotherUser")))
            assertFalse(service.canRead(createEntity("admin", listOf("admin", "anotherUser"),"anotherUser")))
            assertFalse(service.canRead(createEntity("anotherUser", listOf("admin", "anotherUser"),"admin")))
            assertFalse(service.canRead(createEntity("admin", listOf("admin"),"admin")))
        }
    }

    @Test
    fun canReadEntityAdmin() {
        AuthContext.runAs("admin", listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN")) {
            val service = JournalSettingsPermissionsServiceImpl()
            assertTrue(service.canRead(createEntity("user1", "user1")))
            assertTrue(service.canRead(createEntity("user1", "anotherUser")))
            assertTrue(service.canRead(createEntity("anotherUser", "user1")))
            assertTrue(service.canRead(createEntity("GROUP_all", "anotherUser")))
            assertTrue(service.canRead(createEntity("anotherUser", "anotherUser")))
            assertTrue(service.canRead(createEntity("admin", "anotherUser")))
            assertTrue(service.canRead(createEntity("anotherUser", "admin")))
            assertTrue(service.canRead(createEntity("admin", "admin")))


            assertTrue(service.canRead(createEntity("", listOf("user1"), "user1")))
            assertTrue(service.canRead(createEntity("user1", "anotherUser")))
            assertTrue(service.canRead(createEntity("anotherUser", "user1")))
            assertTrue(service.canRead(createEntity("", listOf("GROUP_all"), "anotherUser")))
            assertTrue(service.canRead(createEntity("anotherUser", "anotherUser")))
            assertTrue(service.canRead(createEntity("admin", "anotherUser")))
            assertTrue(service.canRead(createEntity("", listOf("anotherUser"), "admin")))
            assertTrue(service.canRead(createEntity("admin", "admin")))
        }
    }

    @Test
    fun canReadDtoUser() {
        AuthContext.runAs("user1", listOf("GROUP_all", "user1", "ROLE_USER")) {
            val service = JournalSettingsPermissionsServiceImpl()
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("GROUP_all")
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("admin")
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("admin")
                    }
                )
            )
            assertFalse(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("admin")
                        withCreator("admin")
                    }
                )
            )


            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("user1"))
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser", "user1"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser", "admin"))
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("GROUP_all"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser", "admin"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("admin"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser", "admin"))
                        withCreator("admin")
                    }
                )
            )
            assertFalse(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("admin"))
                        withCreator("admin")
                    }
                )
            )
        }
    }

    @Test
    fun canReadDtoAdmin() {
        AuthContext.runAs("admin", listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN")) {
            val service = JournalSettingsPermissionsServiceImpl()
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("GROUP_all")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("admin")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("admin")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthority("admin")
                        withCreator("admin")
                    }
                )
            )


            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("user1", "admin"))
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser", "user1"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser"))
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("GROUP_all"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("admin", "user1"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser"))
                        withCreator("admin")
                    }
                )
            )
            assertTrue(
                service.canRead(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("admin"))
                        withCreator("admin")
                    }
                )
            )
        }
    }

    @Test
    fun canWriteEntityUser() {
        AuthContext.runAs("user1", listOf("GROUP_all", "user1", "ROLE_USER")) {
            val service = JournalSettingsPermissionsServiceImpl()
            assertTrue(service.canWrite(createEntity("user1", "user1")))
            assertFalse(service.canWrite(createEntity("user1", "anotherUser")))
            assertTrue(service.canWrite(createEntity("anotherUser", "user1")))
            assertFalse(service.canWrite(createEntity("GROUP_all", "anotherUser")))
            assertFalse(service.canWrite(createEntity("anotherUser", "anotherUser")))
            assertFalse(service.canWrite(createEntity("admin", "anotherUser")))
            assertFalse(service.canWrite(createEntity("anotherUser", "admin")))
            assertFalse(service.canWrite(createEntity("admin", "admin")))


            assertTrue(service.canWrite(createEntity("user1", listOf("user1"), "user1")))
            assertTrue(service.canWrite(createEntity("", listOf("anotherUser", "user1"), "user1")))

            assertFalse(service.canWrite(createEntity("user1", listOf("user1"), "anotherUser")))
            assertFalse(service.canWrite(createEntity("", listOf("GROUP_all", "user1"), "anotherUser")))
            assertFalse(service.canWrite(createEntity("anotherUser", listOf("anotherUser"), "anotherUser")))
            assertFalse(service.canWrite(createEntity("admin", listOf("admin"), "anotherUser")))
            assertFalse(service.canWrite(createEntity("anotherUser", listOf("anotherUser", "admin"), "admin")))
            assertFalse(service.canWrite(createEntity("", listOf("admin"), "admin")))
        }
    }

    @Test
    fun canWriteEntityAdmin() {
        AuthContext.runAs("admin", listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN")) {
            val service = JournalSettingsPermissionsServiceImpl()
            assertTrue(service.canWrite(createEntity("user1", "user1")))
            assertTrue(service.canWrite(createEntity("user1", "anotherUser")))
            assertTrue(service.canWrite(createEntity("anotherUser", "user1")))
            assertTrue(service.canWrite(createEntity("GROUP_all", "anotherUser")))
            assertTrue(service.canWrite(createEntity("anotherUser", "anotherUser")))
            assertTrue(service.canWrite(createEntity("admin", "anotherUser")))
            assertTrue(service.canWrite(createEntity("anotherUser", "admin")))
            assertTrue(service.canWrite(createEntity("admin", "admin")))


            assertTrue(service.canWrite(createEntity("user1", listOf("user1"), "user1")))
            assertTrue(service.canWrite(createEntity("", listOf("anotherUser", "user1"), "user1")))
            assertTrue(service.canWrite(createEntity("user1", listOf("user1"), "anotherUser")))
            assertTrue(service.canWrite(createEntity("", listOf("GROUP_all", "user1"), "anotherUser")))
            assertTrue(service.canWrite(createEntity("anotherUser", listOf("anotherUser"), "anotherUser")))
            assertTrue(service.canWrite(createEntity("admin", listOf("admin"), "anotherUser")))
            assertTrue(service.canWrite(createEntity("anotherUser", listOf("anotherUser", "admin"), "admin")))
            assertTrue(service.canWrite(createEntity("", listOf("admin"), "admin")))
        }
    }

    @Test
    fun canWriteDtoUser() {
        AuthContext.runAs("user1", listOf("GROUP_all", "user1", "ROLE_USER")) {
            val service = JournalSettingsPermissionsServiceImpl()
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                        withCreator("user1")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("user1")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("GROUP_all")
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("admin")
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("admin")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("admin")
                        withCreator("admin")
                    }
                )
            )


            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("user1"))
                        withCreator("user1")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("user1", "anotherUser"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser"))
                        withCreator("user1")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("GROUP_all", "anotherUser"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser", "admin"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("admin"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser"))
                        withCreator("admin")
                    }
                )
            )
            assertFalse(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("admin"))
                        withCreator("admin")
                    }
                )
            )
        }
    }

    @Test
    fun canWriteDtoAdmin() {
        AuthContext.runAs("admin", listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN")) {
            val service = JournalSettingsPermissionsServiceImpl()
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("GROUP_all")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("admin")
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                        withCreator("admin")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthority("admin")
                        withCreator("admin")
                    }
                )
            )


            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("user1"))
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("user1", "anotherUser"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser"))
                        withCreator("user1")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("GROUP_all", "user1"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("admin"))
                        withCreator("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser"))
                        withCreator("admin")
                    }
                )
            )
            assertTrue(
                service.canWrite(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("admin", "anotherUser"))
                        withCreator("admin")
                    }
                )
            )
        }
    }

    @Test
    fun canWriteNewUser() {
        AuthContext.runAs("user1", listOf("GROUP_all", "user1", "ROLE_USER")) {
            val service = JournalSettingsPermissionsServiceImpl()
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthority("GROUP_all")
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                    }
                )
            )


            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("user1"))
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("user1", "anotherUser"))
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("GROUP_all"))
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser"))
                    }
                )
            )
        }
    }

    @Test
    fun canWriteNewAdmin() {
        AuthContext.runAs("admin", listOf("GROUP_all", "admin", "ROLE_USER", "ROLE_ADMIN")) {
            val service = JournalSettingsPermissionsServiceImpl()
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthority("user1")
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthority("anotherUser")
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthority("GROUP_all")
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthority("admin")
                    }
                )
            )


            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("user1"))
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("anotherUser", "admin"))
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("GROUP_all"))
                    }
                )
            )
            assertTrue(
                service.canWriteNew(
                    JournalSettingsDto.create {
                        withAuthorities(listOf("admin"))
                    }
                )
            )
        }
    }

    private fun createEntity(authority: String, creator: String): JournalSettingsEntity {
        val result = JournalSettingsEntity()
        result.authority = authority
        result.createdBy = creator
        return result
    }

    private fun createEntity(authority: String, authorities: List<String>, creator: String): JournalSettingsEntity {
        val result = JournalSettingsEntity()
        result.authority = authority
        result.authorities = authorities
        result.createdBy = creator
        return result
    }
}
