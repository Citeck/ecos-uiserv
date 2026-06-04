package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.MoveCardAction
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService
import ru.citeck.ecos.uiserv.domain.board.cardorder.test.BoardCardTestFixture
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals

/**
 * Coverage for `type$<typeId>` virtual boards (a board synthesized from a type's statuses,
 * with no saved board record).
 *
 * Disabled: uiserv's `rboard` source does NOT currently synthesize `type$` boards — see
 * [ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao.getRecordAtts], which returns
 * EmptyAttValue for a non-saved board, and [ru.citeck.ecos.uiserv.domain.board.api.records.ResolvedBoardRecordsDao]
 * which then yields null. Until virtual boards are produced (spec §9), this path cannot be exercised.
 * See docs/superpowers/specs/2026-05-12-board-card-drag-design.md.
 */
@Disabled("virtual boards (type\$<typeId>) are not produced by uiserv's rboard source yet")
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardCardOrderVirtualBoardTest {

    @Autowired
    lateinit var service: BoardCardOrderService

    @Autowired
    lateinit var fixture: BoardCardTestFixture

    @BeforeEach
    fun setup() = AuthContext.runAsSystem { fixture.initVirtualBoard() }

    @AfterEach
    fun cleanup() = AuthContext.runAsSystem { fixture.cleanup() }

    @Test
    fun `move and load work on a type-virtual board`() = AuthContext.runAsSystem {
        val before = service.getBoardCards(fixture.boardRef, null, null)
            .first { it.columnId == fixture.firstColumnId }.cards
        service.moveCard(
            MoveCardAction(fixture.boardRef, fixture.card("c1"), fixture.firstColumnId, afterCard = null, cards = before)
        )
        val col = service.getBoardCards(fixture.boardRef, null, null).first { it.columnId == fixture.firstColumnId }
        assertEquals(fixture.card("c1"), col.cards.first())
    }
}
