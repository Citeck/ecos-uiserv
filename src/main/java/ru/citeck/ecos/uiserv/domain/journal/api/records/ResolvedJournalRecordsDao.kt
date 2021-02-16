package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.dto.resolve.ResolvedColumnDef
import ru.citeck.ecos.uiserv.domain.journal.dto.resolve.ResolvedJournalDef
import java.util.*
import kotlin.collections.ArrayList

@Component
class ResolvedJournalRecordsDao(
    private val journalRecordsDao: JournalRecordsDao,
    private val ecosTypeService: EcosTypeService,
    private val columnEditorResolver: ColumnEditorResolver,
    private val columnFormatterResolver: ColumnFormatterResolver
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao {

    override fun getId() = "rjournal"

    override fun queryRecords(query: RecordsQuery): Any? {

        val records = journalRecordsDao.queryRecords(query)

        val result = RecsQueryRes<Any>()
        result.setHasMore(records.getHasMore())
        result.setTotalCount(records.getTotalCount())
        result.setRecords(records.getRecords().map { resolveJournal(it) })
        return result
    }

    override fun getRecordAtts(record: String): Any? {
        return resolveJournal(journalRecordsDao.getRecordAtts(record))
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

        resolvePredicate(journalBuilder)

        return createResolvedDef(journal, journalBuilder, typeInfo)
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

    private fun resolveTypeJournalProps(journal: JournalDef.Builder, typeInfo: EcosTypeInfo?) {

        if (MLText.isEmpty(journal.label)) {
            if (typeInfo != null) {
                journal.withLabel(typeInfo.name)
            }
            if (MLText.isEmpty(journal.label)) {
                journal.withLabel(MLText(journal.id))
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
                JournalColumnDef.create().withName(it.id)
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
            typeInfo?.model?.attributes?.map { it.id to it }?.toMap() ?: emptyMap()

        val attributeEdges = HashMap<String, String>()
        val columnIdxByName = HashMap<String, Int>()

        var columnIdxCounter = 0
        for (column in columns) {

            columnIdxByName[column.name] = columnIdxCounter++

            val attribute = if (column.attribute.isBlank()) {
                column.name
            } else {
                column.attribute
            }
            val isInnerAtt = attribute.contains(".") || attribute.contains("?")

            val typeAtt = typeAtts[column.name]

            val edgeAtts = ArrayList<String>()
            if (column.type == null) {
                val typeAttType = typeAtt?.type
                if (typeAttType != null) {
                    column.withType(typeAttType)
                } else if (!isInnerAtt) {
                    edgeAtts.add("type")
                }
            }
            if (column.editable == null && !isInnerAtt) {
                edgeAtts.add("protected")
            }
            if (MLText.isEmpty(column.label)) {
                val typeAttName = typeAtt?.name
                if (!MLText.isEmpty(typeAttName)) {
                    column.withLabel(typeAttName)
                } else if (!isInnerAtt) {
                    edgeAtts.add("title")
                }
            }
            if (column.multiple == null) {
                val typeAttMultiple = typeAtt?.multiple
                if (typeAttMultiple != null) {
                    column.withMultiple(typeAttMultiple)
                } else if (!isInnerAtt) {
                    edgeAtts.add("multiple")
                }
            }

            if (edgeAtts.isNotEmpty()) {
                if (edgeAtts.size == 1) {
                    edgeAtts.add("javaClass") // protection from optimization
                }
                attributeEdges[column.name] = "_edge.$attribute{${edgeAtts.joinToString(",")}}"
            }
        }

        if (!RecordRef.isEmpty(metaRecord) && attributeEdges.isNotEmpty()) {

            val attributes = recordsService.getAtts(metaRecord, attributeEdges)
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
                        column.withLabel(MLText(value.get("title").asText()))
                    }
                    if (value.has("multiple")) {
                        column.withMultiple(value.get("multiple").asBoolean())
                    }
                }
            }
        }

        columns.forEach { column ->

            if (MLText.isEmpty(column.label)) {
                column.label = MLText(column.name)
            }
            if (column.type == null) {
                column.withType(AttributeType.TEXT);
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
                column.attribute = column.name
            }

            val attType = typeAtts[column.name]
            columnEditorResolver.resolve(column, attType)
            columnFormatterResolver.resolve(column, attType)
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
            if (atts.contains(RecordConstants.ATT_ECOS_TYPE) || atts.contains(RecordConstants.ATT_TYPE)) {
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