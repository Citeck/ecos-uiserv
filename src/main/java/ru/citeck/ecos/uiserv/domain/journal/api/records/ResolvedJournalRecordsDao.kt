package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
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
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.dto.ResolvedJournalDef
import java.util.*
import kotlin.collections.ArrayList

@Component
class ResolvedJournalRecordsDao(
    private val journalRecordsDao: JournalRecordsDao,
    private val ecosTypeService: EcosTypeService
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

        val resolvedDto = ResolvedJournalDef(journal)

        if (journal.journalDef == null || StringUtils.isBlank(journal.journalDef.id)) {
            return resolvedDto
        }
        val journalBuilder = journal.journalDef.copy()

        resolveTypeRef(journalBuilder)

        val typeRef = journalBuilder.typeRef

        var typeInfo: EcosTypeInfo? = null
        if (typeRef != null && RecordRef.isNotEmpty(typeRef)) {
            typeInfo = ecosTypeService.getTypeInfo(typeRef)
        }
        resolveTypeJournalProps(journalBuilder, typeInfo)

        resolveEdgeMeta(journalBuilder, typeInfo)
        resolvePredicate(journalBuilder)

        val newJournal = JournalRecordsDao.JournalRecord(journal)
        newJournal.journalDef = journalBuilder.build()

        return createResolvedDef(newJournal, typeInfo)
    }

    private fun createResolvedDef(journal: JournalRecordsDao.JournalRecord,
                                  typeInfo: EcosTypeInfo?): ResolvedJournalDef {

        val result = ResolvedJournalDef(journal)

        if (typeInfo != null) {
            result.sourceId = typeInfo.sourceId ?: ""
            result.createVariants = typeInfo.createVariants ?: emptyList()
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

        if (typeInfo != null && MLText.isEmpty(journal.label)) {
            journal.label = typeInfo.name
        }
    }

    private fun resolveEdgeMeta(journal: JournalDef.Builder, typeInfo: EcosTypeInfo?) {

        var metaRecord = journal.metaRecord
        if (RecordRef.isEmpty(metaRecord) && typeInfo != null && !typeInfo.sourceId.isNullOrBlank()) {
            metaRecord = RecordRef.valueOf(typeInfo.sourceId + "@")
        }
        if (RecordRef.isEmpty(metaRecord)) {
            return
        }
        val attributeEdges = HashMap<String, String>()
        val columnIdxByName = HashMap<String, Int>()

        var columnIdxCounter = 0
        for (columnDto in journal.columns) {

            columnIdxByName[columnDto.name] = columnIdxCounter++

            val attribute = columnDto.attribute ?: columnDto.name

            if (!attribute.contains(".") && !attribute.contains("?")) {
                val edgeAtts = ArrayList<String>()
                if (StringUtils.isBlank(columnDto.type)) {
                    edgeAtts.add("type")
                }
                if (columnDto.editable == null) {
                    edgeAtts.add("protected")
                }
                if (MLText.isEmpty(columnDto.label)) {
                    edgeAtts.add("title")
                }
                if (columnDto.multiple == null) {
                    edgeAtts.add("multiple")
                }
                if (edgeAtts.isNotEmpty()) {
                    if (edgeAtts.size == 1) {
                        edgeAtts.add("javaClass") // protection from optimization
                    }
                    attributeEdges[columnDto.name] = "_edge.$attribute{${edgeAtts.joinToString(",")}}"
                }
            }
        }
        if (attributeEdges.isEmpty()) {
            return
        }
        val newColumns = ArrayList(journal.columns)

        val attributes = recordsService.getAtts(metaRecord, attributeEdges)
        attributes.forEach { name: String, value: DataValue ->
            val columnIdx = columnIdxByName[name]
            if (columnIdx != null) {
                val newColumn = newColumns[columnIdx].copy()
                newColumn.withType(value.get("type").asText())
                if (StringUtils.isBlank(newColumn.type)) {
                    newColumn.withType("text")
                }
                if (value.has("protected")) {
                    newColumn.withEditable(!value.get("protected").asBoolean())
                }
                if (value.has("title")) {
                    newColumn.withLabel(MLText(value.get("title").asText()))
                }
                if (value.has("multiple")) {
                    newColumn.withMultiple(value.get("multiple").asBoolean())
                }
                newColumns[columnIdx] = newColumn.build()
            }
        }

        journal.withColumns(newColumns)
    }

    private fun resolvePredicate(journal: JournalDef.Builder) {

        val typeRef = journal.typeRef

        if (RecordRef.isEmpty(typeRef)) {
            return
        }

        val fullPredicate: Predicate
        val typePredicate: Predicate = Predicates.eq("_type", typeRef.toString())

        val journalPredicate = journal.predicate
        fullPredicate = if (journalPredicate != null && journalPredicate != VoidPredicate.INSTANCE) {
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
