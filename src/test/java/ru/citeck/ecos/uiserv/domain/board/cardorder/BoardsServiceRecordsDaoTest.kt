package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService
import ru.citeck.ecos.uiserv.domain.board.cardorder.test.BoardCardTestFixture
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardsServiceRecordsDaoTest {

    @Autowired
    lateinit var records: RecordsService

    @Autowired
    lateinit var service: BoardCardOrderService

    @Autowired
    lateinit var fixture: BoardCardTestFixture

    @BeforeEach
    fun setup() = AuthContext.runAsSystem { fixture.init() }

    @AfterEach
    fun cleanup() = AuthContext.runAsSystem { fixture.cleanup() }

    @Test
    fun `move-card action moves the card`() = AuthContext.runAsSystem {
        val before = service.getBoardCards(fixture.boardRef, null, null)
            .first { it.columnId == "col1" }.cards.map { it.toString() }
        records.mutate(
            EntityRef.create("uiserv", "boards-service", ""),
            mapOf(
                "action" to "move-card",
                "config" to mapOf(
                    "board" to fixture.boardRef.toString(),
                    "card" to fixture.card("c1").toString(),
                    "column" to "col1",
                    "afterCard" to null,
                    "cards" to before
                )
            )
        )
        val col1 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(fixture.card("c1"), col1.cards.first())
    }

    @Test
    fun `move-card persists order under the config workspace`() = AuthContext.runAsSystem {
        val before = service.getBoardCards(fixture.boardRef, null, null, "", workspace = "wsX")
            .first { it.columnId == "col1" }.cards.map { it.toString() }
        records.mutate(
            EntityRef.create("uiserv", "boards-service", ""),
            mapOf(
                "action" to "move-card",
                "config" to mapOf(
                    "board" to fixture.boardRef.toString(),
                    "card" to fixture.card("c1").toString(),
                    "column" to "col1",
                    "afterCard" to null,
                    "workspace" to "wsX",
                    "cards" to before
                )
            )
        )
        // The move (c1 -> top) must be visible when reading the SAME workspace it was written in.
        val inWs = service.getBoardCards(fixture.boardRef, null, null, "", workspace = "wsX")
            .first { it.columnId == "col1" }
        assertEquals(fixture.card("c1"), inWs.cards.first())
        // ...and NOT leak into another workspace's view.
        val otherWs = service.getBoardCards(fixture.boardRef, null, null, "", workspace = "wsY")
            .first { it.columnId == "col1" }
        assertEquals(fixture.card("c3"), otherWs.cards.first())
    }

    @Test
    fun `unknown action fails`() {
        assertFailsWith<Exception> {
            AuthContext.runAsSystem {
                records.mutate(
                    EntityRef.create("uiserv", "boards-service", ""),
                    mapOf("action" to "no-such-action", "config" to emptyMap<String, Any>())
                )
            }
        }
    }
}
