package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService.ColumnPageReq
import ru.citeck.ecos.uiserv.domain.board.cardorder.test.BoardCardTestFixture
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.temporal.ChronoUnit
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
    fun `unranked cards come first by status-modified desc`() = AuthContext.runAsSystem {
        // all columns, default page. Cards' _statusModified == _created here (no status changes yet),
        // so the order matches creation-desc: c3, c2, c1.
        val cols = service.getBoardCards(fixture.boardRef, null, null)
        val col1 = cols.first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), col1.cards)
        assertEquals(3L, col1.totalCount)
    }

    @Test
    fun `status change floats a card to the top of the unranked block (status-modified, not created)`() = AuthContext.runAsSystem {
        // d1 is created AFTER c1..c3, so by creation it would sit above c1 in col2. Move c1 (the OLDEST
        // card by _created) into col2 via a status change: its _statusModified is now the newest, so it
        // floats to the very top — proving unranked order keys off _statusModified, not _created.
        val d1 = fixture.createCard("d1", "col2")
        fixture.setStatus("c1", "col2")
        val col2 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col2" }
        assertEquals(listOf(fixture.card("c1"), d1), col2.cards)
    }

    @Test
    fun `never-ranked cards older than the newest rank sink below the ranked block`() = AuthContext.runAsSystem {
        // only c2 is ranked -> anchor = c2's link key. c3 (newer) is the "new" block on top;
        // c1 (older, never ranked) is the tail BELOW the ranked block — still visible, still counted.
        fixture.setOrder("c2", "col1", "n0")
        val col1 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), col1.cards)
        assertEquals(3L, col1.totalCount)
    }

    @Test
    fun `a card that left and re-entered the column loses its stale rank`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "a0") // would pin c1 to the top of the ranked block
        fixture.setStatus("c1", "col2")
        fixture.setStatus("c1", "col1") // back, with a newer _statusModified than the rank's link key
        // the a0 rank is from a previous "life" of the card in this column: ignored -> plain ts-desc order
        val col1 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c1"), fixture.card("c3"), fixture.card("c2")), col1.cards)
    }

    @Test
    fun `stale ranks do not raise the new-block anchor`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "g0")
        fixture.setOrder("c3", "col1", "n0") // newest link key...
        fixture.setStatus("c3", "col2") // ...but the row is stale now (card left the column)
        // the anchor must come from VALID ranks only (= c1): c2 is newer than c1 -> new block, on top.
        // An anchor wrongly taken from c3's stale row would sink c2 into the tail below c1.
        val col1 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c2"), fixture.card("c1")), col1.cards)
        assertEquals(2L, col1.totalCount)
    }

    @Test
    fun `manual order is ignored on a no-drag (readOnly) board`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "a0")
        fixture.setOrder("c2", "col1", "b0")
        // canDrag on (fixture default): anchor = c2's link key -> [c3 (new block), c1 (a0), c2 (b0)]
        val ordered = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c1"), fixture.card("c2")), ordered.cards)
        // canDrag off (readOnly = true): plain query order (status-modified desc), the rank table is not consulted
        fixture.setCardOrderEnabled(false)
        val plain = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), plain.cards)
        assertEquals(3L, plain.totalCount)
    }

    @Test
    fun `grouping view falls back to a valid flat rank when the grouping rank is stale`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "z0", grouping = "assignee") // grouping rank, soon stale
        fixture.setStatus("c1", "col2")
        fixture.setStatus("c1", "col1") // back: the assignee z0 rank is from a previous "life" of the card
        fixture.setOrder("c1", "col1", "g0") // flat rank written AFTER the re-entry: valid
        fixture.setOrder("c2", "col1", "n0") // flat, valid
        // the stale assignee row must NOT shadow c1's valid flat fallback: c1 is ranked g0 (top),
        // c2 ranked n0, c3 (older than the anchor = c1's fresh link key) is the tail
        val col1 = service.getBoardCards(fixture.boardRef, null, null, "assignee").first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), col1.cards)
    }

    @Test
    fun `sub-milli link-key drift does not hide a ranked card under an active filter`() = AuthContext.runAsSystem {
        // live _statusModified gets a sub-milli component; the stored link key is the same instant
        // truncated to millis — valid by the millis-equality rule, but strictly BELOW the live value,
        // so the live card lands in the new-block window (> anchor), not the tail window (<= anchor)
        val live = fixture.statusModifiedOf("c2")!!.plusNanos(500_000)
        fixture.setStatusModified("c2", live)
        fixture.setOrderWithLinkKey("c2", "col1", "n0", live.truncatedTo(ChronoUnit.MILLIS))
        // any non-null filter activates the ranked-confirmation path; this one matches everything in col1
        val filter = Predicates.eq("_status", "col1")
        val col1 = service.getBoardCards(fixture.boardRef, null, filter).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), col1.cards)
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
        // result would be [c3, c1, c2] — distinct from the correct status-modified-desc [c3, c2, c1].
        fixture.setOrder("c1", "col1", "g0", grouping = "assignee")
        fixture.setOrder("c2", "col1", "n0", grouping = "assignee")
        val col1 = service.getBoardCards(fixture.boardRef, null, null, "").first { it.columnId == "col1" }
        // no flat ranks -> unranked by _statusModified desc, grouping ranks not consulted
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
