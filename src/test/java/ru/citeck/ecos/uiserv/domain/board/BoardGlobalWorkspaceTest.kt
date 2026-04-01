package ru.citeck.ecos.uiserv.domain.board

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardGlobalWorkspaceTest {

    @Autowired
    lateinit var service: BoardService

    @Autowired
    lateinit var repository: BoardRepository

    @AfterEach
    fun cleanup() {
        repository.deleteAll()
    }

    @Test
    fun `board saved with admin workspace should be found with empty workspace`() {
        val board = BoardDef()
        board.id = "admin-ws-board"
        board.workspace = "admin\$workspace"
        board.name = MLText("Test Board")
        board.typeRef = EntityRef.valueOf("emodel/type@test-board-type")

        service.save(board)

        val found = service.getBoardById(IdInWs.create("admin-ws-board"))
        assertThat(found).isNotNull
        assertThat(found!!.boardDef.id).isEqualTo("admin-ws-board")
    }

    @Test
    fun `board saved with default workspace should be found with empty workspace`() {
        val board = BoardDef()
        board.id = "default-ws-board"
        board.workspace = "default"
        board.name = MLText("Default Board")
        board.typeRef = EntityRef.valueOf("emodel/type@test-board-type")

        service.save(board)

        val found = service.getBoardById(IdInWs.create("default-ws-board"))
        assertThat(found).isNotNull
        assertThat(found!!.boardDef.id).isEqualTo("default-ws-board")
    }

    @Test
    fun `board saved with regular workspace should NOT be found with empty workspace`() {
        val board = BoardDef()
        board.id = "regular-ws-board"
        board.workspace = "custom-workspace"
        board.name = MLText("WS Board")
        board.typeRef = EntityRef.valueOf("emodel/type@test-board-type")

        service.save(board)

        val foundEmpty = service.getBoardById(IdInWs.create("regular-ws-board"))
        assertThat(foundEmpty).isNull()

        val foundWs = service.getBoardById(IdInWs.create("custom-workspace", "regular-ws-board"))
        assertThat(foundWs).isNotNull
        assertThat(foundWs!!.boardDef.id).isEqualTo("regular-ws-board")
    }
}
