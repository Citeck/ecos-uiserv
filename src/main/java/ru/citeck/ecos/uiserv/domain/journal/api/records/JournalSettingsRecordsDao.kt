package ru.citeck.ecos.uiserv.domain.journal.api.records

import ecos.com.fasterxml.jackson210.annotation.JsonValue
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journal.service.settings.JournalSettingsPermissionsService
import ru.citeck.ecos.uiserv.domain.journal.service.settings.JournalSettingsService
import java.nio.charset.StandardCharsets

@Component
class JournalSettingsRecordsDao(
        private val journalSettingsService: JournalSettingsService,
        private val permService: JournalSettingsPermissionsService
) : AbstractRecordsDao(),
        RecordsQueryDao,
        RecordAttsDao,
        RecordMutateDtoDao<JournalSettingsRecordsDao.JournalSettingsRecord>,
        RecordDeleteDao {

    @Override
    override fun getId(): String = "journal-settings"

    @Override
    override fun getRecordAtts(recordId: String): JournalSettingsRecord {
        val dto = journalSettingsService.getById(recordId) ?: JournalSettingsDto.create { withId(recordId) }
        return JournalSettingsRecord(dto)
    }

    @Override
    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<JournalSettingsRecord> {
        val query = recsQuery.getQuery(RequestPredicate::class.java)
        val journalId = query.journalId ?: ""
        if (StringUtils.isBlank(journalId)) {
            return RecsQueryRes()
        }

        val searchResult = journalSettingsService.searchSettings(journalId)
        return RecsQueryRes<JournalSettingsRecord>().apply {
            setTotalCount(searchResult.size.toLong())
            setHasMore(false)
            setRecords(searchResult.map { JournalSettingsRecord(it) })
        }
    }

    @Override
    override fun getRecToMutate(recordId: String): JournalSettingsRecord {
        return getRecordAtts(recordId)
    }

    @Override
    override fun saveMutatedRec(record: JournalSettingsRecord): String {
        val saved = journalSettingsService.save(record.build())
        return saved.id
    }

    @Override
    override fun delete(recordId: String): DelStatus {
        return if (journalSettingsService.delete(recordId)) {
            DelStatus.OK
        } else {
            DelStatus.ERROR
        }
    }

    inner class JournalSettingsRecord(
            private val base: JournalSettingsDto
    ) : JournalSettingsDto.Builder(base) {
        fun getModuleId(): String? {
            return base.id
        }

        @AttName("?disp")
        fun getDisplayName(): String {
            return base.name
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        fun toNonDefaultJson(): Any {
            return Json.mapper.toNonDefaultJson(this)
        }

        fun getData(): ByteArray {
            return YamlUtils.toNonDefaultString(toNonDefaultJson()).toByteArray(StandardCharsets.UTF_8)
        }

        fun getPermissions(): SettingsPermissions {
            return SettingsPermissions(base)
        }
    }

    inner class SettingsPermissions constructor(
            private val dto: JournalSettingsDto
    ) : AttValue {
        override fun has(name: String): Boolean {
            return if ("Write" == name) {
                permService.canWrite(dto)
            } else {
                permService.canRead(dto)
            }
        }
    }

    data class RequestPredicate(
            val journalId: String?
    )
}