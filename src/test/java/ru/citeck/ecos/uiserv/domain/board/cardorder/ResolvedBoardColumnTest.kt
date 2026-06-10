package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The `rboard` column exposes the computed `additionalFilter` predicate the UI reads and sends back in
 * the board-cards query. This verifies it both serializes through `?json` and reflects the column's
 * `hideOldItems` cutoff (and is null when the column has none).
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class ResolvedBoardColumnTest {

    @Autowired
    lateinit var records: RecordsService

    @Autowired
    lateinit var boardService: BoardService

    private val boardId = "resolvedColumnAddFilterTest"

    @AfterEach
    fun cleanup() {
        AuthContext.runAsSystem { records.delete(EntityRef.valueOf("uiserv/board@$boardId")) }
    }

    class ColAtts {
        var id: String = ""

        @AttName("additionalFilter?json")
        var additionalFilter: DataValue = DataValue.NULL
    }

    @Test
    fun `additionalFilter reflects the hideOldItems cutoff and is null otherwise`() = AuthContext.runAsSystem {
        val def = BoardDef(boardId)
        def.name = MLText("Resolved column additionalFilter test")
        def.columns = listOf(
            BoardColumnDef.create().withId("hidden").withName(MLText("hidden"))
                .withHideOldItems(true).withHideItemsOlderThan("P30D").build(),
            BoardColumnDef.create().withId("plain").withName(MLText("plain")).build()
        )
        boardService.save(def)

        val cols = records.getAtts(EntityRef.valueOf("uiserv/rboard@$boardId"), ColumnsAtts::class.java).columns
        val hidden = cols.first { it.id == "hidden" }
        val plain = cols.first { it.id == "plain" }

        assertTrue(plain.additionalFilter.isNull())
        val p = hidden.additionalFilter
        assertEquals("_statusModified", p.get("att").asText())
        assertEquals("ge", p.get("t").asText())
        assertEquals("-P30D", p.get("val").asText())
    }

    class ColumnsAtts {
        @AttName("columns[]")
        var columns: List<ColAtts> = emptyList()
    }
}
