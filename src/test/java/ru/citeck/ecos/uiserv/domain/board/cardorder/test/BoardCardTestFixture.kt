package ru.citeck.ecos.uiserv.domain.board.cardorder.test

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.status.dto.StatusDef
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.cardorder.repo.BoardCardOrderRepo
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.time.Instant
import java.util.Collections

/**
 * Test fixture for the board card-ordering feature. Sets up:
 * - an in-memory card records source (`board-test-card`) supporting query by `_status` and `_created`;
 * - an ECOS type (`board-test-card-type`) whose sourceId points at that source, with statuses col1/col2;
 * - a saved board (`boardCardDragTest`) with two explicit columns col1/col2 bound to that type.
 *
 * The card source / type are registered once per JVM (shared records service); cards and order
 * records are recreated per test and removed by [cleanup].
 */
@Component
class BoardCardTestFixture(
    private val recordsService: RecordsService,
    private val boardService: BoardService,
    private val workspaceService: WorkspaceService,
    private val typesRegistry: EcosTypesRegistry,
    private val orderRepo: BoardCardOrderRepo
) {

    companion object {
        const val CARD_SOURCE = "board-test-card"
        const val TYPE_ID = "board-test-card-type"
        val TYPE_REF: EntityRef = EntityRef.valueOf("emodel/type@$TYPE_ID")
        const val BOARD_ID = "boardCardDragTest"
        const val WORKSPACE = "default"
        val CREATED_BASE: Instant = Instant.parse("2020-01-01T00:00:00Z")

        private var cardDao: RecordingCardDao? = null
    }

    /**
     * Predicate queries the card source received during this run — for asserting query shape (scope/cap).
     */
    val recordedCardQueries: List<RecordsQuery> get() = cardDao?.recordedQueries() ?: emptyList()

    fun clearRecordedCardQueries() {
        cardDao?.clearRecordedQueries()
    }

    lateinit var boardRef: EntityRef
    var boardLocalId: String = ""
    val workspace: String get() = WORKSPACE
    val firstColumnId: String get() = "col1"

    /** Board id resolved the same way the records layer resolves it (handles default-workspace storage). */
    val boardId: IdInWs get() = workspaceService.convertToIdInWs(boardLocalId)

    private val cardRefs = LinkedHashMap<String, EntityRef>()
    private var createdSeq = 0

    fun init() {
        registerCardSourceAndType()
        saveBoard(listOf("col1", "col2"))
        clearData()
        createCard("c1", "col1")
        createCard("c2", "col1")
        createCard("c3", "col1")
    }

    /** Virtual board: a type with statuses and NO saved board (board id = type$<typeId>). */
    fun initVirtualBoard() {
        registerCardSourceAndType()
        // no saved board; rboard synthesizes columns from the type's statuses
        boardLocalId = "type\$$TYPE_ID"
        boardRef = EntityRef.create(Application.NAME, "board", boardLocalId)
        clearData()
        createCard("c1", "col1")
        createCard("c2", "col1")
        createCard("c3", "col1")
    }

    fun cleanup() {
        clearData()
    }

    fun cleanupCardsOnly() {
        deleteAllCards()
        cardRefs.clear()
    }

    fun card(id: String): EntityRef = cardRefs.getValue(id)

    fun createCard(id: String, status: String): EntityRef {
        val created = CREATED_BASE.plusSeconds(createdSeq.toLong())
        createdSeq++
        val ref = recordsService.create(
            CARD_SOURCE,
            mapOf(
                "id" to id,
                "_status" to status,
                "_created" to created.toString()
            )
        )
        cardRefs[id] = ref
        return ref
    }

    fun setOrder(cardId: String, columnId: String, rankKey: String, grouping: String = "") {
        orderRepo.upsert(boardRef.toString(), WORKSPACE, grouping, card(cardId).toString(), columnId, rankKey)
    }

    fun setStatus(cardId: String, status: String) {
        recordsService.mutate(card(cardId), mapOf("_status" to status))
    }

    fun statusOf(cardId: String): String = recordsService.getAtt(card(cardId), "_status?str").asText()

    // ---- internals ----

    private fun registerCardSourceAndType() {
        if (cardDao == null) {
            val dao = RecordingCardDao(CARD_SOURCE)
            recordsService.register(dao)
            cardDao = dao
        }
        typesRegistry.setValue(
            TYPE_ID,
            TypeDef.create()
                .withId(TYPE_ID)
                .withSourceId(CARD_SOURCE)
                .withModel(
                    TypeModelDef.create()
                        .withStatuses(
                            listOf(
                                StatusDef.create { withId("col1") },
                                StatusDef.create { withId("col2") }
                            )
                        )
                        .build()
                )
                .build()
        )
    }

    private fun saveBoard(columnIds: List<String>) {
        val def = BoardDef(BOARD_ID)
        def.workspace = WORKSPACE
        def.name = MLText("Board card drag test")
        def.typeRef = TYPE_REF
        def.columns = columnIds.map {
            BoardColumnDef.create().withId(it).withName(MLText(it)).build()
        }
        val saved = boardService.save(def).boardDef
        boardLocalId = workspaceService.addWsPrefixToId(saved.id, saved.workspace)
        boardRef = EntityRef.create(Application.NAME, "board", boardLocalId)
    }

    private fun clearData() {
        deleteAllCards()
        cardRefs.clear()
        createdSeq = 0
        if (::boardRef.isInitialized) {
            orderRepo.deleteByBoard(boardRef.toString())
        }
    }

    private fun deleteAllCards() {
        if (cardRefs.isNotEmpty()) {
            recordsService.delete(cardRefs.values.toList())
        }
    }
}

/**
 * In-mem card source that records the predicate queries it receives (so tests can assert query shape) and
 * reports a realistic [RecsQueryRes.getTotalCount] — the full filtered count, not just the page size — the
 * way a real DbRecordsDao does, so board-cards totalCount behaviour is exercised faithfully.
 */
class RecordingCardDao(id: String) : InMemDataRecordsDao(id) {

    private val queries = Collections.synchronizedList(ArrayList<RecordsQuery>())

    override fun queryRecords(recsQuery: RecordsQuery): Any {
        if (recsQuery.language != PredicateService.LANGUAGE_PREDICATE) {
            return super.queryRecords(recsQuery)
        }
        queries.add(recsQuery)
        val page = super.queryRecords(recsQuery) as List<*>
        // Full count: same predicate with no paging (-1 = unlimited). Uses super (not this) so the count
        // probe is NOT itself recorded — recordedQueries must keep only the real page query.
        val total = (
            super.queryRecords(
                recsQuery.copy {
                    withMaxItems(-1)
                    withSkipCount(0)
                }
            ) as List<*>
            ).size
        return RecsQueryRes<Any>().also {
            it.setRecords(page)
            it.setTotalCount(total.toLong())
        }
    }

    fun recordedQueries(): List<RecordsQuery> = synchronized(queries) { ArrayList(queries) }

    fun clearRecordedQueries() = synchronized(queries) { queries.clear() }
}
