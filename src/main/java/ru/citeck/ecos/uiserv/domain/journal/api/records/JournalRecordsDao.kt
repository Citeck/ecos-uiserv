package ru.citeck.ecos.uiserv.domain.journal.api.records

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
import ecos.com.fasterxml.jackson210.annotation.JsonValue
import lombok.Data
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.op.delete.dao.RecordDeleteDao
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.uiserv.domain.journal.service.type.TypeJournalService
import java.util.*

@Component
@RequiredArgsConstructor
class JournalRecordsDao(
    private val journalService: JournalService,
    private val ecosTypeService: EcosTypeService
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao,
    RecordMutateDtoDao<JournalRecordsDao.JournalMutateRec>,
    RecordDeleteDao {

    companion object {
        const val ID = "journal"
    }

    override fun getId() = ID

    override fun queryRecords(query: RecordsQuery): RecsQueryRes<JournalRecord> {

        if (query.language == "site-journals") {
            return RecsQueryRes()
        }

        val result = RecsQueryRes<JournalWithMeta>()

        if (query.language == "by-type") {

            val typeRef = query.getQuery(JournalQueryByTypeRef::class.java).typeRef ?: RecordRef.EMPTY
            val journalRef = ecosTypeService.getJournalRefByTypeRef(typeRef)

            if (RecordRef.isNotEmpty(journalRef)) {
                val dto = journalService.getJournalById(journalRef.id)
                if (dto != null) {
                    result.addRecord(JournalRecord(dto))
                }
            }

        } else {

            if (query.language == PredicateService.LANGUAGE_PREDICATE) {
                val predicate = query.getQuery(Predicate::class.java)
                var max: Int = query.page.maxItems
                if (max <= 0) {
                    max = 10000
                }
                val journals = journalService.getAll(max, query.page.skipCount, predicate)
                result.setRecords(ArrayList(journals))
                result.setTotalCount(journalService.getCount(predicate))
            } else {
                result.setRecords(ArrayList(
                    journalService.getAll(query.page.maxItems, query.page.skipCount))
                )
                result.setTotalCount(journalService.count)
            }
        }

        val res = RecsQueryRes<JournalRecord>()
        res.setTotalCount(result.getTotalCount())
        res.setHasMore(result.getHasMore())
        res.setRecords(result.getRecords().map { JournalRecord(it) })
        return res
    }

    override fun getRecordAtts(record: String): JournalRecord {
        val dto = if (record.isEmpty()) {
            JournalWithMeta()
        } else {
            journalService.getById(record) ?: JournalWithMeta()
        }
        return JournalRecord(dto)
    }

    override fun delete(recordId: String): DelStatus {
        journalService.delete(recordId)
        return DelStatus.OK
    }

    override fun getRecToMutate(recordId: String): JournalMutateRec {
        val dto = journalService.getJournalById(recordId)
        return JournalMutateRec(dto?.journalDef ?: JournalDef.create { withId(recordId) })
    }

    override fun saveMutatedRec(record: JournalMutateRec): String {
        record.localId?.let { record.withId(it) }
        return journalService.save(record.build()).journalDef.id
    }

    @Data
    class JournalQueryByTypeRef(
        val typeRef: RecordRef? = null
    )

    open class JournalRecord(base: JournalWithMeta) : JournalWithMeta(base) {

        fun getModuleId(): String {
            return getLocalId()
        }

        fun getLocalId(): String {
            return journalDef?.id ?: ""
        }

        @AttName("?type")
        fun getType(): RecordRef = RecordRef.valueOf("emodel/type@journal")

        @AttName("?disp")
        fun getDisplayName(): String {
            return MLText.getClosestValue(journalDef.label, RequestContext.getLocale(), journalDef.id)
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        open fun toJson(): Any {
            return mapper.toNonDefaultJson(journalDef)
        }
    }

    class JournalMutateRec(base: JournalDef) : JournalDef.Builder(base) {

        var localId: String? = null

        init {
            localId = base.id
        }

        @JsonProperty("_content")
        fun setContent(content: List<ObjectData>) {
            var base64Content = content[0].get("url", "")
            base64Content = base64Content.replace("^data:application/json;base64,".toRegex(), "")
            val data = mapper.read(Base64.getDecoder().decode(base64Content), ObjectData::class.java)!!
            mapper.applyData(this, data)
        }
    }
}
