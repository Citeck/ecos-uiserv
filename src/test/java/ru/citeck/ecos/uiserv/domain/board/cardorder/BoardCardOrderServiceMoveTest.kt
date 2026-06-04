package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.MoveCardAction
import ru.citeck.ecos.uiserv.domain.board.cardorder.repo.BoardCardOrderRepo
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

    @Autowired
    lateinit var orderRepo: BoardCardOrderRepo

    private fun col1OrderedCards() = orderRepo.findByBoardAndColumn(fixture.boardRef.toString(), "default", "", "col1").map { it.cardRef }

    @BeforeEach
    fun setup() = AuthContext.runAsSystem { fixture.init() }

    @AfterEach
    fun cleanup() = AuthContext.runAsSystem { fixture.cleanup() }

    private fun col1Order() = colOrder("col1")

    /**
     * Column display order as the client would render (and send) it before a move.
     */
    private fun colOrder(columnId: String, grouping: String = "", workspace: String = "") = service.getBoardCards(fixture.boardRef, null, null, grouping, workspace = workspace)
        .first { it.columnId == columnId }.cards

    @Test
    fun `move prunes stale order rows for cards that left the column`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "a0") // c1 ranked in col1
        fixture.setStatus("c1", "col2") // external status change -> c1 left col1, leaving a stale col1 order row
        assertEquals(true, col1OrderedCards().contains(fixture.card("c1").toString()))
        // a move INTO col1 self-cleans the stale c1 row
        service.moveCard(MoveCardAction(fixture.boardRef, fixture.card("c2"), "col1", afterCard = null, cards = colOrder("col1")))
        assertEquals(false, col1OrderedCards().contains(fixture.card("c1").toString()))
    }

    @Test
    fun `move positions the card from the provided list without querying the card source`() = AuthContext.runAsSystem {
        val before = col1Order() // [c3, c2, c1]
        fixture.clearRecordedCardQueries()
        service.moveCard(MoveCardAction(fixture.boardRef, fixture.card("c1"), "col1", afterCard = null, cards = before))
        // the move must NOT re-fetch the column from the card source — it uses the provided list
        assertEquals(emptyList(), fixture.recordedCardQueries)
        assertEquals(listOf(fixture.card("c1"), fixture.card("c3"), fixture.card("c2")), col1Order())
    }

    @Test
    fun `first move in a column materializes the whole column`() = AuthContext.runAsSystem {
        // cards c1<c2<c3 by created -> display order [c3, c2, c1]; move c1 to top
        service.moveCard(MoveCardAction(fixture.boardRef, fixture.card("c1"), "col1", afterCard = null, cards = col1Order()))
        assertEquals(listOf(fixture.card("c1"), fixture.card("c3"), fixture.card("c2")), col1Order())
    }

    @Test
    fun `move within column places card right after the anchor`() = AuthContext.runAsSystem {
        service.moveCard(MoveCardAction(fixture.boardRef, fixture.card("c1"), "col1", afterCard = null, cards = col1Order()))
        // now order is [c1, c3, c2]; move c2 to after c1
        service.moveCard(MoveCardAction(fixture.boardRef, fixture.card("c2"), "col1", afterCard = fixture.card("c1"), cards = col1Order()))
        assertEquals(listOf(fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), col1Order())
    }

    @Test
    fun `move between columns changes _status and positions in target`() = AuthContext.runAsSystem {
        service.moveCard(MoveCardAction(fixture.boardRef, fixture.card("c2"), "col2", afterCard = null, cards = colOrder("col2")))
        assertEquals("col2", fixture.statusOf("c2"))
        assertEquals(listOf(fixture.card("c3"), fixture.card("c1")), col1Order()) // c2 left col1
        val col2 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col2" }
        assertEquals(listOf(fixture.card("c2")), col2.cards)
    }

    @Test
    fun `new card after moves appears on top, next move folds it in`() = AuthContext.runAsSystem {
        service.moveCard(MoveCardAction(fixture.boardRef, fixture.card("c1"), "col1", afterCard = null, cards = col1Order()))
        val c4 = fixture.createCard("c4", "col1") // newest, unranked
        assertEquals(listOf(c4, fixture.card("c1"), fixture.card("c3"), fixture.card("c2")), col1Order())
        // move c3 to bottom -> folds c4 into ranked first
        service.moveCard(MoveCardAction(fixture.boardRef, fixture.card("c3"), "col1", afterCard = fixture.card("c2"), cards = col1Order()))
        assertEquals(listOf(c4, fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), col1Order())
    }

    @Test
    fun `external status change makes card unranked in new column`() = AuthContext.runAsSystem {
        service.moveCard(MoveCardAction(fixture.boardRef, fixture.card("c1"), "col1", afterCard = null, cards = col1Order()))
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
            MoveCardAction(
                fixture.boardRef,
                fixture.card("c3"),
                "col1",
                afterCard = null,
                grouping = "assignee",
                cards = colOrder("col1", grouping = "assignee")
            )
        )

        val assignee = service.getBoardCards(fixture.boardRef, null, null, "assignee").first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c1"), fixture.card("c2")), assignee.cards)

        // the flat order must be unchanged (write isolation per grouping)
        val flat = service.getBoardCards(fixture.boardRef, null, null, "").first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), flat.cards)
    }

    @Test
    fun `order is isolated per workspace`() = AuthContext.runAsSystem {
        // move c1 to top only in workspace wsA
        service.moveCard(
            MoveCardAction(
                fixture.boardRef,
                fixture.card("c1"),
                "col1",
                afterCard = null,
                cards = colOrder("col1", workspace = "wsA")
            ),
            workspace = "wsA"
        )

        // wsA sees the manual order (c1 materialized to top)
        val wsA = service.getBoardCards(fixture.boardRef, null, null, "", workspace = "wsA")
            .first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c1"), fixture.card("c3"), fixture.card("c2")), wsA.cards)

        // wsB has no order of its own -> falls back to created-desc, unaffected by wsA's move
        val wsB = service.getBoardCards(fixture.boardRef, null, null, "", workspace = "wsB")
            .first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), wsB.cards)
    }
}
