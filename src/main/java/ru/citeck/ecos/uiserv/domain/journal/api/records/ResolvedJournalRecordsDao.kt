package ru.citeck.ecos.uiserv.domain.journal.api.records

import mu.KotlinLogging
import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeAttsUtils
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSortByDef
import ru.citeck.ecos.uiserv.domain.journal.dto.resolve.ResolvedColumnDef
import ru.citeck.ecos.uiserv.domain.journal.dto.resolve.ResolvedJournalDef
import java.util.*
import kotlin.collections.ArrayList

@Component
class ResolvedJournalRecordsDao(
    private val journalRecordsDao: JournalRecordsDao,
    private val ecosTypeService: EcosTypeService,
    private val columnEditorResolver: ColumnEditorResolver,
    private val columnFormatterResolver: ColumnFormatterResolver,
    private val columnAttSchemaResolver: ColumnAttSchemaResolver
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun getId() = "rjournal"

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        val records = journalRecordsDao.queryRecords(recsQuery)

        val result = RecsQueryRes<Any>()
        result.setHasMore(records.getHasMore())
        result.setTotalCount(records.getTotalCount())
        result.setRecords(records.getRecords().map { resolveJournal(it) })
        return result
    }

    override fun getRecordAtts(recordId: String): Any? {
        return resolveJournal(journalRecordsDao.getRecordAtts(recordId))
    }

    private fun resolveJournal(journal: JournalRecordsDao.JournalRecord): ResolvedJournalDef {

        if (journal.journalDef == null || StringUtils.isBlank(journal.journalDef.id)) {
            return ResolvedJournalDef(journal) { emptyList() }
        }
        val journalBuilder = journal.journalDef.copy()

        resolveTypeRef(journalBuilder)

        val typeRef = journalBuilder.typeRef

        var typeInfo: EcosTypeInfo? = null
        if (RecordRef.isNotEmpty(typeRef)) {
            typeInfo = ecosTypeService.getTypeInfo(typeRef)
        }
        resolveTypeJournalProps(journalBuilder, typeInfo)

        resolveSorting(journalBuilder)
        resolvePredicate(journalBuilder)
        resolveActionsDef(journalBuilder)
        resolveActionsFromType(journalBuilder, typeInfo)

        return createResolvedDef(journal, journalBuilder, typeInfo)
    }

    private fun resolveActionsFromType(journalBuilder: JournalDef.Builder, typeInfo: EcosTypeInfo?) {
        val actions = ArrayList(journalBuilder.actions)
        val isInherit = journalBuilder.actionsFromType
        if (isInherit == false || isInherit == null && actions.size > 0) {
            return
        }

        typeInfo?.inhActions?.map {
            if (it.id != "record-actions") {
                actions.add(it)
            }
        }
        journalBuilder.withActions(actions.distinct())
    }

    private fun resolveActionsDef(journalBuilder: JournalDef.Builder) {

        val actionsDef = journalBuilder.actionsDef
        if (actionsDef.isEmpty()) {
            return
        }

        val actions = ArrayList(journalBuilder.actions)
        journalBuilder.withActionsDef(actionsDef.map {
            val localId = if (it.id.isBlank()) {
                val actionBytes = Json.mapper.toBytes(it) ?: ByteArray(0)
                DigestUtils.md5DigestAsHex(actionBytes)
            } else {
                it.id
            }
            val action = it.copy()
            action.withId("journal$${journalBuilder.id}$$localId")
            actions.add(RecordRef.create("uiserv", "action", action.id))
            action.build()
        })

        journalBuilder.withActions(actions)
    }

    private fun createResolvedDef(journalRecord: JournalRecordsDao.JournalRecord,
                                  journalBuilder: JournalDef.Builder,
                                  typeInfo: EcosTypeInfo?): ResolvedJournalDef {


        val newJournal = JournalRecordsDao.JournalRecord(journalRecord)
        newJournal.journalDef = journalBuilder.build()

        val result = ResolvedJournalDef(newJournal) { resolveColumns(journalBuilder, typeInfo) }

        if (typeInfo != null) {
            result.createVariants = typeInfo.inhCreateVariants ?: emptyList()
        }

        return result
    }

    private fun resolveTypeRef(journal: JournalDef.Builder) {

        if (RecordRef.isNotEmpty(journal.typeRef)) {
            return
        }

        val journalRef = RecordRef.create("uiserv", JournalRecordsDao.ID, journal.id)
        val typeRef = ecosTypeService.getTypeRefByJournal(journalRef)

        journal.withTypeRef(typeRef)
    }

    private fun resolveSorting(journal: JournalDef.Builder) {
        if (journal.sortBy.isEmpty()) {
            journal.withSortBy(listOf(JournalSortByDef(RecordConstants.ATT_CREATED, false)))
        }
    }

    private fun resolveTypeJournalProps(journal: JournalDef.Builder, typeInfo: EcosTypeInfo?) {

        if (MLText.isEmpty(journal.name)) {
            if (typeInfo != null) {
                journal.withName(typeInfo.name)
            }
            if (MLText.isEmpty(journal.name)) {
                journal.withName(MLText(journal.id))
            }
        }

        if (journal.sourceId.isBlank()) {
            if (typeInfo != null) {
                journal.withSourceId(typeInfo.inhSourceId)
            }
            if (journal.sourceId.isBlank()) {
                journal.withSourceId("alfresco/")
            }
        }

        if (RecordRef.isEmpty(journal.metaRecord)) {
            if (typeInfo != null) {
                journal.withMetaRecord(typeInfo.metaRecord)
            }
            if (RecordRef.isEmpty(journal.metaRecord)) {
                journal.withMetaRecord(RecordRef.valueOf(journal.sourceId + "@"))
            }
        }
    }

    private fun resolveColumns(journal: JournalDef.Builder, typeInfo: EcosTypeInfo?): List<ResolvedColumnDef> {

        val columns = if (journal.columns.isEmpty()) {
            typeInfo?.model?.attributes?.map {
                JournalColumnDef.create().withId(it.id)
            } ?: emptyList()
        } else {
            journal.columns.map { it.copy() }
        }
        return resolveEdgeMetaImpl(journal.metaRecord, columns, typeInfo)
    }

    private fun resolveEdgeMetaImpl(metaRecord: RecordRef,
                                    columns: List<JournalColumnDef.Builder>,
                                    typeInfo: EcosTypeInfo?): List<ResolvedColumnDef> {

        val typeAtts: Map<String, AttributeDef> =
            typeInfo?.model?.getAllAttributes()?.associate { it.id to it } ?: emptyMap()

        val attributeEdges = HashMap<String, String>()
        val columnIdxByName = HashMap<String, Int>()

        var columnIdxCounter = 0
        for (column in columns) {

            columnIdxByName[column.id] = columnIdxCounter++

            val attribute = column.id

            val typeAtt = typeAtts[column.id] ?: EcosTypeAttsUtils.STD_ATTS[column.id]

            val edgeAtts = ArrayList<String>()
            if (column.type == null) {
                val typeAttType = typeAtt?.type
                if (typeAttType != null) {
                    column.withType(typeAttType)
                } else {
                    edgeAtts.add("type")
                }
            }
            if (column.editable == null) {
                edgeAtts.add("protected?bool")
            }
            if (MLText.isEmpty(column.name)) {
                val typeAttName = typeAtt?.name
                if (!MLText.isEmpty(typeAttName)) {
                    column.withName(typeAttName)
                } else {
                    edgeAtts.add("title")
                }
            }
            if (column.multiple == null) {
                val typeAttMultiple = typeAtt?.multiple
                if (typeAttMultiple != null) {
                    column.withMultiple(typeAttMultiple)
                } else {
                    edgeAtts.add("multiple?bool")
                }
            }

            if (edgeAtts.isNotEmpty()) {
                if (edgeAtts.size == 1) {
                    edgeAtts.add("javaClass") // protection from optimization
                }
                attributeEdges[column.id] = "_edge.$attribute{${edgeAtts.joinToString(",")}}"
            }
        }

        if (!RecordRef.isEmpty(metaRecord) && attributeEdges.isNotEmpty()) {

            try {
                val attributes = if (metaRecord.appName == "alfresco") {
                    try {
                        recordsService.getAtts(metaRecord, attributeEdges)
                    } catch (e: Exception) {
                        // todo: solution should be more elegant
                        log.warn { "Exception while metaRecord edge atts request: $metaRecord atts: $attributeEdges" }
                        RecordAtts(metaRecord)
                    }
                } else {
                    recordsService.getAtts(metaRecord, attributeEdges)
                }
                attributes.forEach { name: String, value: DataValue ->
                    val columnIdx = columnIdxByName[name]
                    if (columnIdx != null) {
                        val column = columns[columnIdx]
                        if (value.has("type")) {
                            column.withType(value.get("type").asText())
                        }
                        if (value.has("protected")) {
                            column.withEditable(!value.get("protected").asBoolean())
                        }
                        if (value.has("title")) {
                            column.withName(MLText(value.get("title").asText()))
                        }
                        if (value.has("multiple")) {
                            column.withMultiple(value.get("multiple").asBoolean())
                        }
                    }
                }
            } catch (e: Exception) {
                var cause: Throwable = e
                while (cause.cause != null) {
                    cause = cause.cause!!
                }
                log.warn { "Edge meta can't be resolved for record $metaRecord. Msg: ${cause.message}" }
            }
        }

        columns.forEach { column ->

            if (MLText.isEmpty(column.name)) {
                column.name = MLText(column.id)
            }
            if (column.type == null) {
                column.withType(AttributeType.TEXT)
            }
            if (column.searchable == null) {
                column.searchable = true
            }
            if (column.searchableByText == null) {
                column.searchableByText = column.searchable
            }
            if (column.sortable == null) {
                column.sortable = column.searchable != false && column.type != AttributeType.ASSOC
            }
            if (column.groupable == null) {
                column.groupable = column.type == AttributeType.ASSOC
            }
            if (column.hidden == null) {
                column.hidden = false
            }
            if (column.visible == null) {
                column.visible = true
            }
            if (column.attribute.isBlank()) {
                column.attribute = column.id
            }

            val attType = typeAtts[column.id]
            columnEditorResolver.resolve(column, attType)
            columnFormatterResolver.resolve(column, attType)
            columnAttSchemaResolver.resolve(column, attType)
        }

        return columns.map { ResolvedColumnDef(it.build()) }
    }

    private fun resolvePredicate(journal: JournalDef.Builder) {

        val typeRef = journal.typeRef

        if (RecordRef.isEmpty(typeRef)) {
            return
        }

        val fullPredicate: Predicate
        val typePredicate: Predicate = Predicates.eq("_type", typeRef.toString())

        val journalPredicate = journal.predicate
        fullPredicate = if (journalPredicate != VoidPredicate.INSTANCE) {
            val atts = PredicateUtils.getAllPredicateAttributes(journalPredicate)
            if (atts.contains(RecordConstants.ATT_TYPE)) {
                journalPredicate
            } else {
                Predicates.and(journalPredicate, typePredicate)
            }
        } else {
            typePredicate
        }
        journal.withPredicate(fullPredicate)
    }
}
