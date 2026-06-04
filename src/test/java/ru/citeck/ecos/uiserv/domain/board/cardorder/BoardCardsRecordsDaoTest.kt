package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.test.BoardCardTestFixture
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardCardsRecordsDaoTest {

    @Autowired
    lateinit var records: RecordsService

    @Autowired
    lateinit var fixture: BoardCardTestFixture

    @BeforeEach
    fun setup() = AuthContext.runAsSystem { fixture.init() }

    @AfterEach
    fun cleanup() = AuthContext.runAsSystem { fixture.cleanup() }

    class ColAtts {
        var columnId: String = ""
        var totalCount: Long = 0

        @AttName("cards[]?id")
        var cards: List<String> = emptyList()
    }

    @Test
    fun `no columns arg returns all columns with first page`() = AuthContext.runAsSystem {
        val res = records.query(
            RecordsQuery.create {
                withSourceId("board-cards")
                withQuery(dataObj("board", fixture.boardRef.toString(), "maxItemsPerColumn", 2))
            },
            ColAtts::class.java
        )
        val ids = res.getRecords().map { it.columnId }
        assertEquals(listOf("col1", "col2"), ids.sorted())
        val col1 = res.getRecords().first { it.columnId == "col1" }
        assertEquals(3L, col1.totalCount)
        assertEquals(2, col1.cards.size)
    }

    @Test
    fun `columns arg with skipCount returns a single-column page`() = AuthContext.runAsSystem {
        val res = records.query(
            RecordsQuery.create {
                withSourceId("board-cards")
                withQuery(
                    dataObj(
                        "board",
                        fixture.boardRef.toString(),
                        "columns",
                        listOf(mapOf("id" to "col1", "skipCount" to 2, "maxItems" to 2))
                    )
                )
            },
            ColAtts::class.java
        )
        assertEquals(1, res.getRecords().size)
        val col1 = res.getRecords().first()
        assertEquals("col1", col1.columnId)
        assertEquals(1, col1.cards.size)
        assertEquals(3L, col1.totalCount)
    }

    @Test
    fun `filter is AND-ed into the column query`() = AuthContext.runAsSystem {
        // a filter contradicting the column status must yield zero cards (proves the AND-composition)
        val res = records.query(
            RecordsQuery.create {
                withSourceId("board-cards")
                withQuery(
                    dataObj(
                        "board",
                        fixture.boardRef.toString(),
                        "columns",
                        listOf(mapOf("id" to "col1")),
                        "filter",
                        mapOf("t" to "eq", "att" to "_status", "val" to "col2")
                    )
                )
            },
            ColAtts::class.java
        )
        val col1 = res.getRecords().first { it.columnId == "col1" }
        assertEquals(0L, col1.totalCount)
        assertEquals(0, col1.cards.size)
    }

    @Test
    fun `column defaults to a 25-card page when maxItems is not specified`() = AuthContext.runAsSystem {
        fixture.cleanupCardsOnly()
        repeat(60) { fixture.createCard("d$it", "col1") }
        val res = records.query(
            RecordsQuery.create {
                withSourceId("board-cards")
                withQuery(dataObj("board", fixture.boardRef.toString(), "columns", listOf(mapOf("id" to "col1"))))
            },
            ColAtts::class.java
        )
        val col1 = res.getRecords().first { it.columnId == "col1" }
        assertEquals(25, col1.cards.size)
        // totalCount is the TRUE column size (from the query COUNT), not the fetched window — so the UI's
        // "load more" knows there are 60 cards even though only the first 25-card page was returned.
        assertEquals(60L, col1.totalCount)
    }

    @Test
    fun `explicit maxItems below the floor is raised to 25`() = AuthContext.runAsSystem {
        fixture.clearRecordedCardQueries()
        records.query(
            RecordsQuery.create {
                withSourceId("board-cards")
                withQuery(
                    dataObj("board", fixture.boardRef.toString(), "columns", listOf(mapOf("id" to "col1", "maxItems" to 5)))
                )
            },
            ColAtts::class.java
        )
        // maxItems 5 < floor 25 -> fetch floored to 25, independent of the default page size
        assertEquals(25, fixture.recordedCardQueries.last().page.maxItems)
    }

    @Test
    fun `card query fetch defaults to the 25-card floor`() = AuthContext.runAsSystem {
        fixture.clearRecordedCardQueries()
        records.query(
            RecordsQuery.create {
                withSourceId("board-cards")
                withQuery(dataObj("board", fixture.boardRef.toString(), "columns", listOf(mapOf("id" to "col1"))))
            },
            ColAtts::class.java
        )
        val cardQuery = fixture.recordedCardQueries.last()
        assertEquals(25, cardQuery.page.maxItems)
    }

    @Test
    fun `fetch adds skipCount on top of the page floor`() = AuthContext.runAsSystem {
        fixture.clearRecordedCardQueries()
        records.query(
            RecordsQuery.create {
                withSourceId("board-cards")
                withQuery(
                    dataObj(
                        "board",
                        fixture.boardRef.toString(),
                        "columns",
                        listOf(mapOf("id" to "col1", "skipCount" to 10, "maxItems" to 5))
                    )
                )
            },
            ColAtts::class.java
        )
        // skipCount(10) + max(floor 25, maxItems 5) = 35, NOT max(25, 10+5) = 25
        assertEquals(35, fixture.recordedCardQueries.last().page.maxItems)
    }

    @Test
    fun `request maxItems above the floor raises the fetch limit`() = AuthContext.runAsSystem {
        fixture.cleanupCardsOnly()
        repeat(100) { fixture.createCard("d$it", "col1") }
        fixture.clearRecordedCardQueries()
        val res = records.query(
            RecordsQuery.create {
                withSourceId("board-cards")
                withQuery(
                    dataObj("board", fixture.boardRef.toString(), "columns", listOf(mapOf("id" to "col1", "maxItems" to 80)))
                )
            },
            ColAtts::class.java
        )
        val col1 = res.getRecords().first { it.columnId == "col1" }
        assertEquals(80, col1.cards.size)
        assertEquals(80, fixture.recordedCardQueries.last().page.maxItems)
    }

    @Test
    fun `card query is scoped to the viewing workspace`() = AuthContext.runAsSystem {
        fixture.clearRecordedCardQueries()
        records.query(
            RecordsQuery.create {
                withSourceId("board-cards")
                withWorkspaces(listOf("myws"))
                withQuery(dataObj("board", fixture.boardRef.toString(), "columns", listOf(mapOf("id" to "col1"))))
            },
            ColAtts::class.java
        )
        val cardQuery = fixture.recordedCardQueries.last()
        assertEquals(listOf("myws"), cardQuery.workspaces)
    }

    private fun dataObj(vararg kv: Any): ObjectData {
        val d = ObjectData.create()
        var i = 0
        while (i < kv.size) {
            d.set(kv[i] as String, kv[i + 1])
            i += 2
        }
        return d
    }
}
