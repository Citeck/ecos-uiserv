package ru.citeck.ecos.uiserv.domain.journalsettings.api.records

import com.fasterxml.jackson.annotation.JsonValue
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
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
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsPermissionsService
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsService
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets
import java.time.Instant

@Component
class JournalSettingsRecordsDao(
    private val journalSettingsService: JournalSettingsService,
    private val permService: JournalSettingsPermissionsService,
    private val authoritiesApi: EcosAuthoritiesApi
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao,
    RecordMutateDtoDao<JournalSettingsRecordsDao.JournalSettingsRecord>,
    RecordDeleteDao {

    @Override
    override fun getId(): String = "journal-settings"

    @Override
    override fun getRecordAtts(recordId: String): JournalSettingsRecord {
        val dto = journalSettingsService.getById(recordId)
            ?: EntityWithMeta(JournalSettingsDto.create { withId(recordId) })
        return JournalSettingsRecord(dto)
    }

    @Override
    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<JournalSettingsRecord> {

        if (recsQuery.language == PredicateService.LANGUAGE_PREDICATE) {

            val predicate = recsQuery.getQuery(Predicate::class.java)
            val settingsDto = journalSettingsService.findAll(
                predicate,
                recsQuery.page.maxItems,
                recsQuery.page.skipCount,
                recsQuery.sortBy
            )
            val result = RecsQueryRes<JournalSettingsRecord>()
            result.setRecords(settingsDto.map { JournalSettingsRecord(it) })
            result.setTotalCount(journalSettingsService.getCount(predicate))

            return result
        }

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

    inner class JournalSettingsRecord(private val originalDto: EntityWithMeta<JournalSettingsDto>) :
        JournalSettingsDto.Builder(originalDto.entity) {

        fun getModuleId(): String {
            return id
        }

        @AttName("?disp")
        fun getDisplayName(): MLText {
            return name
        }

        @JsonValue
        fun toNonDefaultJson(): Any {
            return Json.mapper.toNonDefaultJson(originalDto.entity)
        }

        fun getData(): ByteArray {
            return YamlUtils.toNonDefaultString(toNonDefaultJson()).toByteArray(StandardCharsets.UTF_8)
        }

        fun getPermissions(): SettingsPermissions {
            return SettingsPermissions(this)
        }

        @AttName(RecordConstants.ATT_CREATED)
        fun getCreated(): Instant {
            return originalDto.meta.created
        }

        @AttName(RecordConstants.ATT_CREATOR)
        fun getCreatorAtt(): EntityRef {
            return authoritiesApi.getPersonRef(creator)
        }

        @AttName(RecordConstants.ATT_MODIFIED)
        fun getModified(): Instant {
            return originalDto.meta.modified
        }

        @AttName(RecordConstants.ATT_MODIFIER)
        fun getModifier(): EntityRef {
            return authoritiesApi.getPersonRef(originalDto.meta.modifier)
        }
    }

    inner class SettingsPermissions constructor(
        private val record: JournalSettingsRecord
    ) : AttValue {
        override fun has(name: String): Boolean {
            return when (name) {
                "Write" -> permService.canWrite(record.build())
                "Read" -> permService.canRead(record.build())
                else -> false
            }
        }
    }

    data class RequestPredicate(
        val journalId: String?
    )
}
