package ru.citeck.ecos.uiserv.domain.menu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.menu.api.records.MenuRecords
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.service.testutils.MenuTestBase

class MenuWorkspaceTest : MenuTestBase() {

    @Test
    fun test() {
        deployMenuArtifacts()

        fun assertDefaultConfigsForUser(user: String, authorities: List<String> = emptyList()) {
            assertThat(findMenuId(user, authorities, "")).isEqualTo("default-menu-v1")
            assertThat(findMenuId(user, authorities, "user\$admin")).isEqualTo("default-personal-ws-menu")
            assertThat(findMenuId(user, authorities, "custom-ws")).isEqualTo("default-ws-menu")
        }
        assertDefaultConfigsForUser("admin")

        menuService.save(createMenu("test-ws-0", "ws-0", listOf("admin")))

        assertDefaultConfigsForUser("admin")
        assertDefaultConfigsForUser("pushkin")

        assertThat(findMenuId("admin", emptyList(), "ws-0")).isEqualTo("test-ws-0")
    }

    private fun findMenuId(user: String, authorities: List<String>, workspace: String): String {
        return AuthContext.runAsFull(user, authorities) {
            records.queryOne(
                RecordsQuery.create()
                    .withSourceId(MenuRecords.ID)
                    .withQuery(
                        DataValue.createObj()
                            .set("workspace", workspace)
                            .set("user", user)
                            .set("version", 1)
                    ).build()
            )?.getLocalId() ?: ""
        }
    }

    private fun createMenu(id: String, workspace: String, authorities: List<String>): MenuDto {
        val menu = MenuDto.create()
            .withId(id)
            .withWorkspace(workspace)
            .withVersion(1)

        if (authorities.isNotEmpty()) {
            menu.withAuthorities(authorities)
        }

        return menu.build()
    }
}
