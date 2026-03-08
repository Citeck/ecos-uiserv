package ru.citeck.ecos.uiserv.domain.menu.api.records

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.UUID

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class MenuRecordsTest {

    companion object {
        private const val RECORD_SOURCE = "menu@"
    }

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var menuService: MenuService

    @BeforeEach
    fun setup() {
        menuService.getAllMenus()
            .filter { !menuService.isDefaultMenu(it.id) }
            .forEach { menuService.deleteByExtId(it.id) }
    }

    @Test
    fun createdAndModifiedAttributesAreReturned() {
        val id = UUID.randomUUID().toString()
        recordsService.mutate(RECORD_SOURCE, mapOf("id" to id, "authorities" to listOf("GROUP_EVERYONE")))

        val atts = recordsService.getAtts(
            RECORD_SOURCE + id,
            mapOf("created" to "_created", "modified" to "_modified")
        )

        assertThat(atts.get("created").asText()).isNotBlank()
        assertThat(atts.get("modified").asText()).isNotBlank()
    }

    @Test
    fun creatorAndModifierAttributesAreReturned() {
        val id = UUID.randomUUID().toString()
        recordsService.mutate(RECORD_SOURCE, mapOf("id" to id, "authorities" to listOf("GROUP_EVERYONE")))

        val atts = recordsService.getAtts(
            RECORD_SOURCE + id,
            mapOf("creator" to "_creator?id", "modifier" to "_modifier?id")
        )

        assertThat(atts.get("creator").asText()).isNotBlank()
        assertThat(atts.get("modifier").asText()).isNotBlank()
    }
}
