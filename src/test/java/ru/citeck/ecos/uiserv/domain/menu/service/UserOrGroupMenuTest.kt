package ru.citeck.ecos.uiserv.domain.menu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.service.testutils.MenuTestBase

class UserOrGroupMenuTest : MenuTestBase() {

    @Test
    fun test() {

        configs.setConfig("menu-group-priority", listOf(DataValue.create("{\"id\":\"user-group\"}")))
        /*
        records.register(
            RecordsDaoBuilder.create(ConfigRecords.ID)
                .addRecord("menu-group-priority", MenuGroupPriorityConfig(listOf("user-group")))
                .build()
        )*/

        val userMenu = MenuDto("test-user-menu")
        userMenu.authorities = listOf("user")
        userMenu.version = 1

        menuService.save(userMenu)

        val userMenuFromService = AuthContext.runAs("user", listOf("user-group")) {
            menuService.getMenuForCurrentUser(1)
        }

        assertThat(userMenuFromService?.id).isEqualTo("test-user-menu")

        val userGroupMenu = MenuDto("test-user-group-menu")
        userGroupMenu.authorities = listOf("user-group")
        userGroupMenu.version = 1

        menuService.save(userGroupMenu)

        val userGroupMenuFromService = AuthContext.runAs("user", listOf("user-group")) {
            menuService.getMenuForCurrentUser(1)
        }

        assertThat(userGroupMenuFromService?.id).isEqualTo(userMenu.id)

        val userGroupMenuFromService2 = AuthContext.runAs("user2", listOf("user-group")) {
            menuService.getMenuForCurrentUser(1)
        }

        assertThat(userGroupMenuFromService2?.id).isEqualTo(userGroupMenu.id)
    }

    class MenuGroupPriorityConfig(
        val value: List<String>
    )
}
