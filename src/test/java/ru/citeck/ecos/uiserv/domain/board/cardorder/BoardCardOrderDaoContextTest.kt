package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardCardOrderDaoContextTest {

    @Autowired
    lateinit var recordsService: RecordsService

    @Test
    fun `can create, read and query a board-card-order record`() {
        val ref = recordsService.create(
            BoardCardOrderDesc.SOURCE_ID,
            mapOf(
                BoardCardOrderDesc.ATT_BOARD_REF to "uiserv/board@ws1\$b1",
                BoardCardOrderDesc.ATT_CARD_REF to "emodel/x@card1",
                BoardCardOrderDesc.ATT_COLUMN_ID to "col1",
                BoardCardOrderDesc.ATT_RANK_KEY to "g0"
            )
        )
        assertEquals("g0", recordsService.getAtt(ref, BoardCardOrderDesc.ATT_RANK_KEY).asText())

        val found = recordsService.query(
            RecordsQuery.create {
                withSourceId(BoardCardOrderDesc.SOURCE_ID)
                withQuery(Predicates.eq(BoardCardOrderDesc.ATT_BOARD_REF, "uiserv/board@ws1\$b1"))
            }
        )
        assertEquals(1, found.getRecords().size)
    }
}
