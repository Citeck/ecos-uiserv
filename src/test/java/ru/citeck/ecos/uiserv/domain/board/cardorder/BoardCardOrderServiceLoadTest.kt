package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService.ColumnPageReq
import ru.citeck.ecos.uiserv.domain.board.cardorder.test.BoardCardTestFixture
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardCardOrderServiceLoadTest {

    @Autowired
    lateinit var service: BoardCardOrderService

    @Autowired
    lateinit var fixture: BoardCardTestFixture

    @BeforeEach
    fun setup() = AuthContext.runAsSystem { fixture.init() }

    @AfterEach
    fun cleanup() = AuthContext.runAsSystem { fixture.cleanup() }

    @Test
    fun `unranked cards come first by created desc`() = AuthContext.runAsSystem {
        // all columns, default page
        val cols = service.getBoardCards(fixture.boardRef, null, null)
        val col1 = cols.first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), col1.cards)
        assertEquals(3L, col1.totalCount)
    }

    @Test
    fun `ranked cards follow unranked, by rankKey asc`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "g0")
        fixture.setOrder("c2", "col1", "n0")
        // c3 has no order record -> unranked, on top
        val cols = service.getBoardCards(fixture.boardRef, null, null)
        val col1 = cols.first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c1"), fixture.card("c2")), col1.cards)
    }

    @Test
    fun `pagination respects per-column page size and skipCount`() = AuthContext.runAsSystem {
        val page1 = service.getBoardCards(fixture.boardRef, listOf(ColumnPageReq("col1", 0, 2)), null)
            .first { it.columnId == "col1" }
        assertEquals(2, page1.cards.size)
        assertEquals(3L, page1.totalCount)

        val page2 = service.getBoardCards(fixture.boardRef, listOf(ColumnPageReq("col1", 2, 2)), null)
            .first { it.columnId == "col1" }
        assertEquals(1, page2.cards.size)
        assertEquals(3L, page2.totalCount)
    }

    @Test
    fun `single-column request returns only that column`() = AuthContext.runAsSystem {
        val cols = service.getBoardCards(fixture.boardRef, listOf(ColumnPageReq("col1", 0, 25)), null)
        assertEquals(listOf("col1"), cols.map { it.columnId })
    }

    @Test
    fun `flat order ignores grouping-specific ranks`() = AuthContext.runAsSystem {
        // grouping ranks order c1(g0) before c2(n0); if the flat view erroneously used them the
        // result would be [c3, c1, c2] — distinct from the correct created-desc [c3, c2, c1].
        fixture.setOrder("c1", "col1", "g0", grouping = "assignee")
        fixture.setOrder("c2", "col1", "n0", grouping = "assignee")
        val col1 = service.getBoardCards(fixture.boardRef, null, null, "").first { it.columnId == "col1" }
        // no flat ranks -> unranked by _created desc, grouping ranks not consulted
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), col1.cards)
    }

    @Test
    fun `grouping view falls back to the flat order when not ranked in the grouping`() = AuthContext.runAsSystem {
        // arrange a flat order, then view under a grouping with no grouping-specific ranks
        fixture.setOrder("c1", "col1", "g0", grouping = "")
        fixture.setOrder("c2", "col1", "n0", grouping = "")
        // c3 unranked everywhere -> on top; then flat-ordered c1, c2
        val col1 = service.getBoardCards(fixture.boardRef, null, null, "assignee").first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c1"), fixture.card("c2")), col1.cards)
    }

    @Test
    fun `grouping-specific rank overrides the flat fallback`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "g0", grouping = "")
        fixture.setOrder("c2", "col1", "n0", grouping = "")
        // under "assignee", pin c2 above c1 (overriding flat); c1 keeps its flat fallback
        fixture.setOrder("c2", "col1", "a0", grouping = "assignee")
        val col1 = service.getBoardCards(fixture.boardRef, null, null, "assignee").first { it.columnId == "col1" }
        // c3 unranked on top; then c2 (a0) before c1 (g0 via fallback)
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), col1.cards)
    }
}
