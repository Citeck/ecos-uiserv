package ru.citeck.ecos.uiserv.domain.journal.api.records

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.annotation.PostConstruct
import lombok.Data
import lombok.RequiredArgsConstructor
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.context.lib.auth.AuthContext.isRunAsAdmin
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.events2.type.RecordEventsService
import ru.citeck.ecos.records2.predicate.PredicateService
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
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalActionDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta
import ru.citeck.ecos.uiserv.domain.journal.registry.JournalsRegistryConfiguration
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.uiserv.domain.journal.service.JournalServiceImpl
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets

@Component
@RequiredArgsConstructor
class JournalRecordsDao(
    private val journalService: JournalService,
    private val ecosTypeService: EcosTypeService,
    private val recordEventsService: RecordEventsService
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao,
    RecordMutateDtoDao<JournalRecordsDao.JournalMutateRec>,
    RecordDeleteDao {

    companion object {
        const val ID = "journal"
    }

    @PostConstruct
    fun init() {
        journalService.onJournalChanged { before, after ->
            recordEventsService.emitRecChanged(before, after, getId()) {
                JournalRecord(JournalWithMeta(it))
            }
        }
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<JournalRecord> {

        if (recsQuery.language == "site-journals") {
            return RecsQueryRes()
        }

        val result = RecsQueryRes<JournalWithMeta>()

        if (recsQuery.language == "by-type") {

            val typeRef = recsQuery.getQuery(JournalQueryByTypeRef::class.java).typeRef ?: EntityRef.EMPTY
            val journalRef = ecosTypeService.getJournalRefByTypeRef(typeRef)

            if (EntityRef.isNotEmpty(journalRef)) {
                val dto = journalService.getJournalById(journalRef.getLocalId())
                if (dto != null) {
                    result.addRecord(JournalRecord(dto))
                }
            }
        } else {

            if (recsQuery.language == PredicateService.LANGUAGE_PREDICATE) {

                val registryQuery = recsQuery.copy()
                    .withSourceId(JournalsRegistryConfiguration.JOURNALS_REGISTRY_SOURCE_ID)
                    .build()
                val queryRes = recordsService.query(registryQuery)

                val journals = journalService.getAll(queryRes.getRecords().map { it.getLocalId() }.toSet())
                result.setRecords(ArrayList(journals))
                result.setTotalCount(queryRes.getTotalCount())
            } else {
                result.setRecords(
                    ArrayList(
                        journalService.getAll(recsQuery.page.maxItems, recsQuery.page.skipCount)
                    )
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

    override fun getRecordAtts(recordId: String): JournalRecord {
        val dto = if (recordId.isEmpty()) {
            JournalWithMeta(false)
        } else {
            journalService.getJournalById(recordId) ?: JournalWithMeta(false)
        }
        return JournalRecord(dto)
    }

    @Secured(AuthRole.ADMIN, AuthRole.SYSTEM)
    override fun delete(recordId: String): DelStatus {
        journalService.delete(recordId)
        return DelStatus.OK
    }

    @Secured(AuthRole.ADMIN, AuthRole.SYSTEM)
    override fun getRecToMutate(recordId: String): JournalMutateRec {
        val dto = journalService.getJournalById(recordId)
        return JournalMutateRec(dto?.journalDef ?: JournalDef.create().withId(recordId).build())
    }

    override fun saveMutatedRec(record: JournalMutateRec): String {
        record.actionsDef.forEach {
            val config = it.config
            config.forEach { k, v ->
                if (v.isTextual()) {
                    val value = v.asText()
                    if (StringUtils.isNotBlank(value) && value[0] == '{' && value[value.length - 1] == '}') {
                        val convertedValue = DataValue.of(mapper.convert(v, ObjectNode::class.java))
                        if (convertedValue.isObject()) {
                            config[k] = convertedValue
                        }
                    }
                }
            }
        }

        return journalService.save(record.build()).journalDef.id
    }

    @Data
    class JournalQueryByTypeRef(
        val typeRef: EntityRef? = null
    )

    class ActionDefRecord(
        @AttName("...")
        val actionDef: JournalActionDef
    ) {
        fun getConfigMap(): Map<String, String> {

            val map = HashMap<String, String>()
            actionDef.config.forEach { k, v ->
                map[k] = if (v.isObject()) {
                    v.toString()
                } else {
                    v.asText()
                }
            }
            return map
        }

        fun getAsJson(): Any {
            return mapper.toNonDefaultJson(actionDef)
        }
    }

    open class JournalRecord(base: JournalWithMeta) : JournalWithMeta(base) {

        fun getModuleId(): String {
            return getLocalId()
        }

        fun getLocalId(): String {
            return journalDef?.id ?: ""
        }

        @AttName("?type")
        fun getType(): EntityRef = EntityRef.valueOf("emodel/type@journal")

        fun getActionsDef(): List<ActionDefRecord> {
            return journalDef?.actionsDef?.map { ActionDefRecord(it) } ?: emptyList()
        }

        @AttName("?disp")
        fun getDisplayName(): MLText {
            return journalDef?.name ?: MLText.EMPTY
        }

        fun getWorkspaceRef(): EntityRef? {
            return journalDef?.workspace?.let {
                if (it.isBlank()) {
                    EntityRef.EMPTY
                } else {
                    EntityRef.create(AppName.EMODEL, "workspace", it)
                }
            }
        }

        @JsonValue
        open fun toNonDefaultJson(): Any {
            return mapper.toNonDefaultJson(journalDef)
        }

        open fun getColumns(): List<Any> {
            return journalDef?.columns?.map { ColumnAttValue(it) } ?: emptyList()
        }

        open fun getData(): ByteArray {
            return YamlUtils.toNonDefaultString(toNonDefaultJson()).toByteArray(StandardCharsets.UTF_8)
        }

        open fun getPermissions(): Permissions? {
            return Permissions()
        }

        inner class Permissions : AttValue {

            override fun has(name: String): Boolean {
                return if (name.equals("write", ignoreCase = true)) {
                    val journalId: String = getLocalId()
                    if (journalId.contains("$")) {
                        false
                    } else {
                        isRunAsAdmin() && !JournalServiceImpl.SYSTEM_JOURNALS.contains(journalId)
                    }
                } else {
                    name.equals("read", ignoreCase = true)
                }
            }
        }
    }

    class ColumnAttValue(
        @AttName("...")
        val column: JournalColumnDef
    ) {
        fun getJsonValue(): ObjectData {
            return ObjectData.create(column)
        }
    }

    class JournalMutateRec(base: JournalDef) : JournalDef.Builder(base) {

        val originalId = base.id

        fun withModuleId(id: String) {
            withId(id)
        }

        fun withWorkspaceRef(workspaceRef: EntityRef) {
            withWorkspace(workspaceRef.getLocalId())
        }
    }
}
