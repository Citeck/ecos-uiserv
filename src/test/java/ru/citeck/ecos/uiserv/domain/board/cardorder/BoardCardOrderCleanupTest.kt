package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.repo.BoardCardOrderRepo
import ru.citeck.ecos.uiserv.domain.board.cardorder.test.BoardCardTestFixture
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardCardOrderCleanupTest {

    @Autowired
    lateinit var boardService: BoardService

    @Autowired
    lateinit var repo: BoardCardOrderRepo

    @Autowired
    lateinit var fixture: BoardCardTestFixture

    @BeforeEach
    fun setup() = AuthContext.runAsSystem { fixture.init() }

    @AfterEach
    fun cleanup() = AuthContext.runAsSystem { fixture.cleanupCardsOnly() }

    @Test
    fun `deleting a board removes its order records`() = AuthContext.runAsSystem {
        repo.upsert(fixture.boardRef.toString(), "", fixture.card("c1").toString(), "col1", "g0")
        assertEquals(1, repo.findByBoard(fixture.boardRef.toString()).size)

        boardService.delete(fixture.boardId)
        assertEquals(0, repo.findByBoard(fixture.boardRef.toString()).size)
    }
}
