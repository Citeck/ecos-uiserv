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
import kotlin.test.assertEquals

/**
 * Display contract of a curated column: `[statused after the last curation, ts desc]` ++
 * `[ranked, rankKey asc]` ++ `[older never-ranked, ts desc]`. The "last curation" anchor is the max
 * `orderedAt` over the column's valid rank rows — all fixture timestamps (cards, rows, the service's
 * curation clock) share one synthetic monotonic scale, so before/after relations are deterministic.
 */
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
    fun `cards statused after the last curation float above, the rest sink to the tail`() = AuthContext.runAsSystem {
        fixture.setOrder("c2", "col1", "n0") // the curation act = the anchor
        val d1 = fixture.createCard("d1", "col1") // statused AFTER the curation -> the new block, on top
        // c3/c1 were statused BEFORE the curation and left unranked -> the tail, below the ranked block
        val col1 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(d1, fixture.card("c2"), fixture.card("c3"), fixture.card("c1")), col1.cards)
        assertEquals(4L, col1.totalCount)
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
    fun `stale rows do not contribute their curation time to the anchor`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "g0") // the last VALID curation
        val d1 = fixture.createCard("d1", "col1") // statused after it
        fixture.setOrder("c3", "col1", "z0") // a newer curation...
        fixture.setStatus("c3", "col2") // ...whose row is stale now (the card left the column)
        // the anchor must come from VALID rows only (c1's curation): d1 is the new block on top.
        // The stale row's newer curation time would wrongly sink d1 into the tail below c1.
        val col1 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(d1, fixture.card("c1"), fixture.card("c2")), col1.cards)
        assertEquals(3L, col1.totalCount)
    }

    @Test
    fun `manual order is ignored on a no-drag (readOnly) board`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "a0")
        fixture.setOrder("c2", "col1", "b0")
        // canDrag on (fixture default): ranked [c1, c2], pre-curation c3 in the tail
        val ordered = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), ordered.cards)
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
        // c2 ranked n0, c3 (statused before the flat curations) is the tail
        val col1 = service.getBoardCards(fixture.boardRef, null, null, "assignee").first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), col1.cards)
    }

    @Test
    fun `rows without a link key are ignored (no legacy support)`() = AuthContext.runAsSystem {
        // a row not produced by this code (pre-link-key leftover) is simply stale: the column renders
        // as if the row didn't exist; the next move into the column deletes it
        fixture.setOrderWithLinkKey("c2", "col1", "n0", null, null)
        val col1 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), col1.cards)
    }

    @Test
    fun `cards statused within the skew margin before the curation still float above`() = AuthContext.runAsSystem {
        // the anchor is on uiserv's clock, card timestamps on the card source's: a card statused a
        // sub-margin moment "before" the curation may actually be newer — it must not sink into the tail
        val curationAt = fixture.statusModifiedOf("c3")!!.plusMillis(300)
        fixture.setOrderWithLinkKey("c2", "col1", "n0", fixture.statusModifiedOf("c2"), curationAt)
        // boundary = curation - 500ms < ts(c3): c3 floats above the ranked block; c1 (a full second older) is the tail
        val col1 = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), col1.cards)
    }

    @Test
    fun `a ranked card ahead of the anchor (clock skew) is not hidden under an active filter`() = AuthContext.runAsSystem {
        // the card's _statusModified (card-source clock) is ahead of the row's orderedAt (uiserv clock):
        // the valid ranked card lands in the new-block query window, not the tail window — with a filter
        // active it must still be confirmed as matching (via the union of both windows)
        val live = fixture.statusModifiedOf("c2")!!
        fixture.setOrderWithLinkKey("c2", "col1", "n0", live, live.minusMillis(1))
        val filter = Predicates.eq("_status", "col1")
        val col1 = service.getBoardCards(fixture.boardRef, null, filter).first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c3"), fixture.card("c2"), fixture.card("c1")), col1.cards)
    }

    @Test
    fun `ranked cards order by rankKey asc`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "g0")
        fixture.setOrder("c2", "col1", "n0")
        // c3 has no rank and was statused before the curations -> the tail
        val cols = service.getBoardCards(fixture.boardRef, null, null)
        val col1 = cols.first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), col1.cards)
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
        // result would be [c1, c2, c3] — distinct from the correct status-modified-desc [c3, c2, c1].
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
        // the flat baseline is inherited: ranked [c1, c2]; pre-curation c3 in the tail
        val col1 = service.getBoardCards(fixture.boardRef, null, null, "assignee").first { it.columnId == "col1" }
        assertEquals(listOf(fixture.card("c1"), fixture.card("c2"), fixture.card("c3")), col1.cards)
    }

    @Test
    fun `grouping-specific rank overrides the flat fallback`() = AuthContext.runAsSystem {
        fixture.setOrder("c1", "col1", "g0", grouping = "")
        fixture.setOrder("c2", "col1", "n0", grouping = "")
        // under "assignee", pin c2 above c1 (overriding flat); c1 keeps its flat fallback
        fixture.setOrder("c2", "col1", "a0", grouping = "assignee")
        val col1 = service.getBoardCards(fixture.boardRef, null, null, "assignee").first { it.columnId == "col1" }
        // ranked: c2 (a0) before c1 (g0 via fallback); pre-curation c3 in the tail
        assertEquals(listOf(fixture.card("c2"), fixture.card("c1"), fixture.card("c3")), col1.cards)
    }
}
