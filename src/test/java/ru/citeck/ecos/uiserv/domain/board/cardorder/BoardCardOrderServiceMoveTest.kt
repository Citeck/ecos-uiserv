package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.MoveCardConfig
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService
import ru.citeck.ecos.uiserv.domain.board.cardorder.test.BoardCardTestFixture
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardCardOrderServiceMoveTest {

    @Autowired
    lateinit var service: BoardCardOrderService

    @Autowired
    lateinit var fixture: BoardCardTestFixture

    @BeforeEach
    fun setup() = AuthContext.runAsSystem { fixture.init() }

    @AfterEach
    fun cleanup() = AuthContext.runAsSystem { fixture.cleanup() }

    private fun col1Order() = service.getBoardCards(fixture.boardRef, null, null)
        .first { it.columnId == "col1" }.cards

    @Test
    fun `first move in a column materializes the whole column`() = AuthContext.runAsSystem {
        // cards c1<c2<c3 by created -> display order [c3, c2, c1]; move c1 to top
        service.moveCard(MoveCardConfig(fixture.boardRef, fixture.card("c1"), "col1", afterCard = null))
        assertEquals(listOf(fixture.card("c1"), fixture.card("c3"), fixture.card("c2")), col1Order())
    }

    @Test
    fun `move within column places card right after the anchor`() = AuthContext.runAsSystem {
        service.moveCard(MoveCardConfig(fixture.boardRef, fixture.card("c1"), "col1", afterCard = null))
        // now order is [c1, c3, c2]; move c2 to after c1
        service.moveCard(MoveCardConfig(fixture.boardRef, fixture.card("c2"), "col1", afterCard = fixture.card("c1")))
        assertEquals(listOf(fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), col1Order())
    }

    @Test
    fun `move between columns changes _status and positions in target`() = AuthContext.runAsSystem {
        service.moveCard(MoveCardConfig(fixture.boardRef, fixture.card("c2"), "col2", afterCard = null))
        assertEquals("col2", fixture.statusOf("c2"))
        assertEquals(listOf(fixture.card("c3"), fixture.card("c1")), col1Order()) // c2 left col1
        val col2 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col2" }
        assertEquals(listOf(fixture.card("c2")), col2.cards)
    }

    @Test
    fun `new card after moves appears on top, next move folds it in`() = AuthContext.runAsSystem {
        service.moveCard(MoveCardConfig(fixture.boardRef, fixture.card("c1"), "col1", afterCard = null))
        val c4 = fixture.createCard("c4", "col1") // newest, unranked
        assertEquals(listOf(c4, fixture.card("c1"), fixture.card("c3"), fixture.card("c2")), col1Order())
        // move c3 to bottom -> folds c4 into ranked first
        service.moveCard(MoveCardConfig(fixture.boardRef, fixture.card("c3"), "col1", afterCard = fixture.card("c2")))
        assertEquals(listOf(c4, fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), col1Order())
    }

    @Test
    fun `external status change makes card unranked in new column`() = AuthContext.runAsSystem {
        service.moveCard(MoveCardConfig(fixture.boardRef, fixture.card("c1"), "col1", afterCard = null))
        fixture.setStatus("c1", "col2") // bypass move API
        val cols = service.getBoardCards(fixture.boardRef, null, null)
        assertEquals(listOf(fixture.card("c1")), cols.first { it.columnId == "col2" }.cards)
        assertEquals(false, cols.first { it.columnId == "col1" }.cards.contains(fixture.card("c1")))
    }

    @Test
    fun `move under a grouping inherits flat baseline and leaves flat order untouched`() = AuthContext.runAsSystem {
        // establish a flat manual order: c1, c2, c3
        fixture.setOrder("c1", "col1", "g0")
        fixture.setOrder("c2", "col1", "n0")
        fixture.setOrder("c3", "col1", "t0")

        // first move under "assignee": view inherits the flat baseline [c1,c2,c3]; send c3 to top
        service.moveCard(
            MoveCardConfig(fixture.boardRef, fixture.card("c3"), "col1", afterCard = null, grouping = "assignee")
        )

        val assignee = service.getBoardCards(fixture.boardRef, null, null, "assignee").first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c1"), fixture.card("c2")), assignee.cards)

        // the flat order must be unchanged (write isolation per grouping)
        val flat = service.getBoardCards(fixture.boardRef, null, null, "").first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), flat.cards)
    }
}
