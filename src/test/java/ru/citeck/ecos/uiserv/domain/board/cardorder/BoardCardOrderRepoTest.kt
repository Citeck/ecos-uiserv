package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.repo.BoardCardOrderRepo
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardCardOrderRepoTest {

    @Autowired
    lateinit var repo: BoardCardOrderRepo

    private val board = "uiserv/board@wsX\$repoTest"

    @Test
    fun `upsert is idempotent per board+grouping+card and queries work`() {
        repo.upsert(board, "", "emodel/x@c1", "col1", "g0")
        repo.upsert(board, "", "emodel/x@c2", "col1", "n0")
        repo.upsert(board, "", "emodel/x@c1", "col1", "h0") // update same (board, grouping, card)

        val byBoard = repo.findByBoard(board).sortedBy { it.rankKey }
        assertEquals(listOf("h0", "n0"), byBoard.map { it.rankKey })

        val c1 = repo.findByBoardAndCard(board, "", "emodel/x@c1")
        assertEquals("h0", c1?.rankKey)

        val byColumn = repo.findByBoardAndColumn(board, "", "col1").sortedBy { it.rankKey }
        assertEquals(2, byColumn.size)

        assertNull(repo.findByBoardAndCard(board, "", "emodel/x@missing"))
    }

    @Test
    fun `order is independent per grouping`() {
        val grpBoard = "uiserv/board@wsX\$repoGrouping"
        repo.upsert(grpBoard, "", "emodel/x@g1", "col1", "g0")
        repo.upsert(grpBoard, "assignee", "emodel/x@g1", "col1", "z9")

        // same (board, card) but different grouping -> two independent rows
        assertEquals("g0", repo.findByBoardAndCard(grpBoard, "", "emodel/x@g1")?.rankKey)
        assertEquals("z9", repo.findByBoardAndCard(grpBoard, "assignee", "emodel/x@g1")?.rankKey)

        assertEquals(1, repo.findByBoardAndColumn(grpBoard, "", "col1").size)
        assertEquals(1, repo.findByBoardAndColumn(grpBoard, "assignee", "col1").size)

        repo.deleteByBoard(grpBoard)
    }

    @Test
    fun `deleteByBoard removes all rows of a board across groupings`() {
        val delBoard = "uiserv/board@wsX\$repoDelete"
        repo.upsert(delBoard, "", "emodel/x@d1", "col1", "g0")
        repo.upsert(delBoard, "assignee", "emodel/x@d1", "col1", "g0")
        repo.deleteByBoard(delBoard)
        assertEquals(0, repo.findByBoard(delBoard).size)
    }
}
