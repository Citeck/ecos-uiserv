package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry

@Component
class AddColumnVariantsDao(
    private val typesRegistry: EcosTypesRegistry
) : RecordsQueryDao, RecordAttsDao, AbstractRecordsDao() {

    companion object {
        const val SRC_ID = "journal-column-variants"

        private const val TYPE_ATT_COLUMN_PREFIX = "type-att$"
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        if (recsQuery.language != "predicate-with-data") {
            return null
        }

        val submission = recsQuery.query["/data/data"].getAs(JournalFormSubmission::class.java) ?: return null

        val sourceId = if (submission.sourceId.isNullOrBlank()) {
            typesRegistry.getValue(submission.typeRef?.getLocalId())?.sourceId
        } else {
            submission.sourceId
        }
        if (sourceId.isNullOrBlank()) {
            return null
        }

        if (sourceId.indexOf("/") != -1) {
            val targetSrcRef = sourceId.substring(0, sourceId.indexOf("/") + 1) +
                "source@" + sourceId.substringAfter("/")

            val columnsCustomSrcId = recordsService.getAtt(targetSrcRef, "columnsSourceId").asText()

            if (columnsCustomSrcId.isNotBlank()) {
                return recordsService.query(recsQuery.withSourceId(columnsCustomSrcId))
            }
        }
        val typeDef = typesRegistry.getValue(submission.typeRef?.getLocalId()) ?: return null

        val columnVariants = typeDef.model.attributes.map {
            createAttColumnRecord(typeDef, it)
        }

        val predicate = recsQuery.query["predicate"].getAs(Predicate::class.java) ?: Predicates.alwaysTrue()

        val result = predicateService.filterAndSort(
            columnVariants,
            predicate,
            recsQuery.sortBy,
            recsQuery.page.skipCount,
            recsQuery.page.maxItems
        )

        val queryRes = RecsQueryRes<ColumnRecord>()
        queryRes.setRecords(result)
        queryRes.setTotalCount(columnVariants.size.toLong())

        return queryRes
    }

    override fun getRecordAtts(recordId: String): Any? {
        if (!recordId.startsWith(TYPE_ATT_COLUMN_PREFIX)) {
            return null
        }
        val typeIdAndAttId = recordId.substring(TYPE_ATT_COLUMN_PREFIX.length)
        val typeId = typeIdAndAttId.substringBefore("$")
        val attId = typeIdAndAttId.substringAfter("$")

        val typeDef = typesRegistry.getValue(typeId) ?: return null
        val attDef = typeDef.model.attributes.find { it.id == attId } ?: return null

        return createAttColumnRecord(typeDef, attDef)
    }

    private fun createAttColumnRecord(type: TypeDef, att: AttributeDef): ColumnRecord {
        return ColumnRecord(
            TYPE_ATT_COLUMN_PREFIX + type.id + "$" + att.id,
            att.id,
            att.name,
            att.type.toString()
        )
    }

    override fun getId(): String {
        return SRC_ID
    }

    class JournalFormSubmission(
        val sourceId: String? = null,
        val typeRef: EntityRef? = null
    )

    class ColumnRecord(
        val id: String,
        val columnId: String,
        val name: MLText,
        val type: String
    )
}


