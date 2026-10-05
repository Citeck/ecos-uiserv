package ru.citeck.ecos.uiserv.domain.board

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.citeck.ecos.apps.app.domain.handler.ArtifactDeployMeta
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.ModelServiceFactory
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardMutRecord
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecord
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta
import ru.citeck.ecos.uiserv.domain.board.eapps.BoardArtifactHandler
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.context.WorkspaceApiMock
import java.nio.charset.StandardCharsets
import java.util.function.BiConsumer

/**
 * COREDEV-580: a board of a workspace type without its own journal/form points at the type's auto
 * journal/form (`type$<ws>:<type>`) — the ws prefix after `type$` must become `CURRENT_WS:` on export
 * and be bound to the target workspace on import, like any other ws-scoped ref.
 *
 * WorkspaceApiMock maps workspace id == workspace system id, so a `ws-a:` prefix means workspace `ws-a`.
 */
class BoardWorkspaceRefsTest {

    companion object {
        private const val WS_A = "ws-a"
        private const val WS_B = "ws-b"
    }

    private lateinit var workspaceService: WorkspaceService
    private lateinit var boardService: BoardService
    private lateinit var handler: BoardArtifactHandler

    @BeforeEach
    fun before() {
        val modelServices = ModelServiceFactory()
        modelServices.setWorkspaceApi(WorkspaceApiMock())
        workspaceService = modelServices.workspaceService

        boardService = mock()
        whenever(boardService.save(any())).doAnswer { BoardWithMeta(it.getArgument<BoardDef>(0)) }
        handler = BoardArtifactHandler(boardService, workspaceService)
    }

    @Test
    fun `artifact export replaces ws prefix after type$ with placeholder`() {

        val listener = argumentCaptor<BiConsumer<BoardDef?, BoardDef>>()
        val exported = ArrayList<BoardDef>()
        handler.listenChanges { board, _ -> exported.add(board) }
        verify(boardService).onBoardChanged(listener.capture())

        listener.firstValue.accept(null, wsBoard())

        assertRefs(exported.single(), "CURRENT_WS:")
    }

    @Test
    fun `artifact deploy binds placeholder after type$ to the target workspace`() {

        handler.deployArtifact(exportedBoard(), WS_B)

        assertRefs(savedBoard(), "$WS_B:")
    }

    @Test
    fun `artifact deploy binds auto journal of a co-deployed type`() {

        val board = exportedBoard()
        board.journalRef = EntityRef.valueOf("uiserv/journal@type\$co-type")

        val meta = ArtifactDeployMeta.create()
            .withCoDeployedArtifacts(listOf(EntityRef.valueOf("emodel/type@co-type")))
            .build()
        ArtifactDeployMeta.doWithMeta(meta) { handler.deployArtifact(board, WS_B) }

        assertThat(savedBoard().journalRef.toString()).isEqualTo("uiserv/journal@type\$$WS_B:co-type")
    }

    @Test
    fun `record data export replaces ws prefix after type$ with placeholder`() {

        val record = BoardRecord(BoardWithMeta(wsBoard()), workspaceService)
        val data = YamlUtils.read(String(record.data, StandardCharsets.UTF_8))

        assertRefs(Json.mapper.convert(data, BoardDef::class.java)!!, "CURRENT_WS:")
    }

    @Test
    fun `records upload binds placeholder after type$ to the target workspace`() {

        val record = BoardMutRecord("board", workspaceService)
        record.workspace = WS_B
        record.typeRef = EntityRef.valueOf("emodel/type@CURRENT_WS:type")
        record.journalRef = EntityRef.valueOf("uiserv/journal@type\$CURRENT_WS:type")
        record.cardFormRef = EntityRef.valueOf("uiserv/form@type\$CURRENT_WS:type")

        AuthContext.runAsSystem { BoardRecordsDao(boardService, workspaceService).saveMutatedRec(record) }

        assertRefs(savedBoard(), "$WS_B:")
    }

    private fun assertRefs(board: BoardDef, prefix: String) {
        assertThat(board.typeRef.toString()).isEqualTo("emodel/type@${prefix}type")
        assertThat(board.journalRef.toString()).isEqualTo("uiserv/journal@type\$${prefix}type")
        assertThat(board.cardFormRef.toString()).isEqualTo("uiserv/form@type\$${prefix}type")
    }

    private fun wsBoard(): BoardDef {
        val board = BoardDef("board")
        board.workspace = WS_A
        board.typeRef = EntityRef.valueOf("emodel/type@$WS_A:type")
        board.journalRef = EntityRef.valueOf("uiserv/journal@type\$$WS_A:type")
        board.cardFormRef = EntityRef.valueOf("uiserv/form@type\$$WS_A:type")
        return board
    }

    private fun exportedBoard(): BoardDef {
        val board = BoardDef("board")
        board.typeRef = EntityRef.valueOf("emodel/type@CURRENT_WS:type")
        board.journalRef = EntityRef.valueOf("uiserv/journal@type\$CURRENT_WS:type")
        board.cardFormRef = EntityRef.valueOf("uiserv/form@type\$CURRENT_WS:type")
        return board
    }

    private fun savedBoard(): BoardDef {
        val captor = argumentCaptor<BoardDef>()
        verify(boardService).save(captor.capture())
        return captor.firstValue
    }
}
