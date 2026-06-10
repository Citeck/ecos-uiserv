package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.repo.BoardCardOrderRepo
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardCardOrderRepoTest {

    @Autowired
    lateinit var repo: BoardCardOrderRepo

    private val board = "uiserv/board@repoTest"
    private val ws = "default"

    @Test
    fun `upsert is idempotent per board+grouping+card and queries work`() = AuthContext.runAsSystem {
        repo.upsert(board, ws, "", "emodel/x@c1", "col1", "g0")
        repo.upsert(board, ws, "", "emodel/x@c2", "col1", "n0")
        repo.upsert(board, ws, "", "emodel/x@c1", "col1", "h0") // update same (board, grouping, card)

        val byBoard = repo.findByBoard(board).sortedBy { it.rankKey }
        assertEquals(listOf("h0", "n0"), byBoard.map { it.rankKey })

        val c1 = repo.findByBoardAndCard(board, ws, "", "emodel/x@c1")
        assertEquals("h0", c1?.rankKey)

        val byColumn = repo.findByBoardAndColumn(board, ws, "", "col1").sortedBy { it.rankKey }
        assertEquals(2, byColumn.size)

        assertNull(repo.findByBoardAndCard(board, ws, "", "emodel/x@missing"))

        repo.deleteByBoard(board)
    }

    @Test
    fun `order is independent per grouping`() = AuthContext.runAsSystem {
        val grpBoard = "uiserv/board@repoGrouping"
        repo.upsert(grpBoard, ws, "", "emodel/x@g1", "col1", "g0")
        repo.upsert(grpBoard, ws, "assignee", "emodel/x@g1", "col1", "z9")

        // same (board, card) but different grouping -> two independent rows
        assertEquals("g0", repo.findByBoardAndCard(grpBoard, ws, "", "emodel/x@g1")?.rankKey)
        assertEquals("z9", repo.findByBoardAndCard(grpBoard, ws, "assignee", "emodel/x@g1")?.rankKey)

        assertEquals(1, repo.findByBoardAndColumn(grpBoard, ws, "", "col1").size)
        assertEquals(1, repo.findByBoardAndColumn(grpBoard, ws, "assignee", "col1").size)

        repo.deleteByBoard(grpBoard)
    }

    @Test
    fun `order is independent per workspace`() = AuthContext.runAsSystem {
        val wsBoard = "uiserv/board@repoWorkspace"
        // same (board, grouping, card) but different order workspace -> two isolated rows
        repo.upsert(wsBoard, "wsA", "", "emodel/x@w1", "col1", "a0")
        repo.upsert(wsBoard, "wsB", "", "emodel/x@w1", "col1", "b0")

        // workspace-scoped reads see only their own row
        assertEquals("a0", repo.findByBoardAndCard(wsBoard, "wsA", "", "emodel/x@w1")?.rankKey)
        assertEquals("b0", repo.findByBoardAndCard(wsBoard, "wsB", "", "emodel/x@w1")?.rankKey)
        assertEquals(1, repo.findByBoardAndColumn(wsBoard, "wsA", "", "col1").size)
        assertEquals(1, repo.findByBoardAndColumn(wsBoard, "wsB", "", "col1").size)

        // board cleanup is workspace-agnostic: rows of both workspaces are removed
        assertEquals(2, repo.findByBoard(wsBoard).size)
        repo.deleteByBoard(wsBoard)
        assertEquals(0, repo.findByBoard(wsBoard).size)
    }

    @Test
    fun `upsert stores the card's status-modified link key`() = AuthContext.runAsSystem {
        val lkBoard = "uiserv/board@repoLinkKey"
        val ts = Instant.parse("2020-01-01T00:00:01Z")
        repo.upsert(lkBoard, ws, "", "emodel/x@t1", "col1", "g0", ts)
        repo.upsert(lkBoard, ws, "", "emodel/x@t2", "col1", "n0") // no link key (legacy row shape)

        val rows = repo.findByBoardAndColumn(lkBoard, ws, "", "col1").sortedBy { it.rankKey }
        assertEquals(ts, rows[0].cardStatusModified)
        assertNull(rows[1].cardStatusModified)

        repo.deleteByBoard(lkBoard)
    }

    @Test
    fun `deleteByBoard removes all rows of a board across groupings`() = AuthContext.runAsSystem {
        val delBoard = "uiserv/board@repoDelete"
        repo.upsert(delBoard, ws, "", "emodel/x@d1", "col1", "g0")
        repo.upsert(delBoard, ws, "assignee", "emodel/x@d1", "col1", "g0")
        repo.deleteByBoard(delBoard)
        assertEquals(0, repo.findByBoard(delBoard).size)
    }

    @Test
    fun `column read truncates at the cap deterministically by rankKey`() = AuthContext.runAsSystem {
        val capBoard = "uiserv/board@repoCap"
        val prevCap = repo.maxItems
        try {
            repo.upsert(capBoard, ws, "", "emodel/x@r1", "col1", "z0")
            repo.upsert(capBoard, ws, "", "emodel/x@r2", "col1", "a0")
            repo.upsert(capBoard, ws, "", "emodel/x@r3", "col1", "n0")
            repo.maxItems = 2
            // the TOP of the curated order survives the cap (rankKey asc), not an arbitrary slice
            assertEquals(listOf("a0", "n0"), repo.findByBoardAndColumn(capBoard, ws, "", "col1").map { it.rankKey })
        } finally {
            repo.maxItems = prevCap
            repo.deleteByBoard(capBoard)
        }
    }

    @Test
    fun `deleteByBoard removes all rows even beyond the query cap`() = AuthContext.runAsSystem {
        val bigBoard = "uiserv/board@repoBigDelete"
        val prevCap = repo.maxItems
        try {
            repeat(5) { repo.upsert(bigBoard, ws, "", "emodel/x@b$it", "col1", "k$it") }
            // one capped read sees only 2 of 5 rows — the delete must loop until nothing is left
            repo.maxItems = 2
            repo.deleteByBoard(bigBoard)
            assertEquals(0, repo.findByBoard(bigBoard).size)
        } finally {
            repo.maxItems = prevCap
            repo.deleteByBoard(bigBoard)
        }
    }
}
