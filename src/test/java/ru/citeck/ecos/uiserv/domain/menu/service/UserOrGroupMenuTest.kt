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

        val userMenu = MenuDto.create().withId("test-user-menu")
        userMenu.withAuthorities(listOf("user"))
        userMenu.withVersion(1)

        menuService.save(userMenu.build())

        val userMenuFromService = AuthContext.runAs("user", listOf("user-group")) {
            menuService.getMenuForCurrentUser(1)
        }

        assertThat(userMenuFromService?.id).isEqualTo("test-user-menu")

        val userGroupMenu = MenuDto.create().withId("test-user-group-menu")
        userGroupMenu.withAuthorities(listOf("user-group"))
        userGroupMenu.withVersion(1)

        menuService.save(userGroupMenu.build())

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
