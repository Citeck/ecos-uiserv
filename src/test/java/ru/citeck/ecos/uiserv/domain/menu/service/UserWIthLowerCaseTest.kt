package ru.citeck.ecos.uiserv.domain.menu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.uiserv.TestUtils
import ru.citeck.ecos.uiserv.domain.config.api.records.ConfigRecords
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.service.testutils.MenuTestBase

class UserWIthLowerCaseTest : MenuTestBase() {

    @Test
    fun test() {

        records.register(RecordsDaoBuilder.create(ConfigRecords.ID)
            .addRecord("menu-group-priority", MenuGroupPriorityConfig(listOf("user-group")))
            .build())

        val userMenu = MenuDto("test-user-menu")
        userMenu.authorities = listOf("user")
        userMenu.version = 1

        menuService.save(userMenu)

        val userMenuFromService = TestUtils.runAsUser("User", listOf("user-group")) {
            menuService.getMenuForCurrentUser(1)
        }

        assertThat(userMenuFromService?.id).isEqualTo("test-user-menu")
    }

    class MenuGroupPriorityConfig(
        val value: List<String>
    )
}
