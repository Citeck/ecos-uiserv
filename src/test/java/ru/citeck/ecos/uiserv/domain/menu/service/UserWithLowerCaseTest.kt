package ru.citeck.ecos.uiserv.domain.menu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.uiserv.domain.config.api.records.ConfigRecords
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.service.testutils.MenuTestBase

class UserWithLowerCaseTest : MenuTestBase() {

    @Test
    fun test() {

        records.register(
            RecordsDaoBuilder.create(ConfigRecords.ID)
                .addRecord("menu-group-priority", MenuGroupPriorityConfig(listOf("user-group")))
                .build()
        )

        testImpl(listOf("User"), "user", emptyList())
        testImpl(listOf("User"), "uSer", emptyList())
        testImpl(listOf("user"), "USer", emptyList())
        testImpl(listOf("user"), "user", emptyList())
        testImpl(listOf("USER"), "USER", emptyList())
        testImpl(listOf("USER"), "user", emptyList())
        testImpl(listOf("user"), "USER", emptyList())

        testImpl(listOf("Group0", "Group1"), "user", listOf("group0"))
        testImpl(listOf("Group0", "Group1"), "user", listOf("group1"))
        testImpl(listOf("Group0", "Group1"), "user", listOf("Group0"))
        testImpl(listOf("Group0", "Group1"), "user", listOf("Group1"))
    }

    fun testImpl(menuAuthorities: List<String>, searchUser: String, searchUserGroups: List<String>) {

        val userMenu = MenuDto("test-user-menu")
        userMenu.authorities = menuAuthorities
        userMenu.version = 1

        menuService.save(userMenu)

        val userMenuFromService = AuthContext.runAs(searchUser, searchUserGroups) {
            menuService.getMenuForCurrentUser(1)
        }

        assertThat(userMenuFromService?.id).isEqualTo("test-user-menu")
    }

    class MenuGroupPriorityConfig(
        val value: List<String>
    )
}
