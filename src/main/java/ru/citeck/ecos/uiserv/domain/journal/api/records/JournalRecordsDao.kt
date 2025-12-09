package ru.citeck.ecos.uiserv.domain.journal.api.records

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.annotation.PostConstruct
import lombok.Data
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthContext.isRunAsAdmin
import ru.citeck.ecos.events2.type.RecordEventsService
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
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
import ru.citeck.ecos.uiserv.domain.workspace.service.WorkspaceUiService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets

@Component
@RequiredArgsConstructor
class JournalRecordsDao(
    private val journalService: JournalService,
    private val ecosTypeService: EcosTypeService,
    private val recordEventsService: RecordEventsService,
    private val workspaceService: WorkspaceService,
    private val workspaceUiService: WorkspaceUiService,
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
            if (after != null) {
                recordEventsService.emitRecChanged(before, after, getId()) {
                    JournalRecord(JournalWithMeta(it), workspaceService)
                }
            }
        }
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<JournalRecord> {

        if (recsQuery.language == "site-journals") {
            return RecsQueryRes()
        }

        var recsQuery = recsQuery
        val result = RecsQueryRes<JournalWithMeta>()

        if (recsQuery.language == "by-type") {

            val typeRef = recsQuery.getQuery(JournalQueryByTypeRef::class.java).typeRef ?: EntityRef.EMPTY
            val journalRef = ecosTypeService.getJournalRefByTypeRef(typeRef)

            if (EntityRef.isNotEmpty(journalRef)) {
                val idInWs = workspaceService.convertToIdInWs(journalRef.getLocalId())
                val dto = journalService.getJournalById(idInWs)
                if (dto != null) {
                    result.addRecord(JournalRecord(dto, workspaceService))
                }
            }
        } else {

            recsQuery = workspaceUiService.prepareQueryWithSystemFilterIfRequired(recsQuery)

            if (recsQuery.language == PredicateService.LANGUAGE_PREDICATE) {

                val queryRes = recordsService.query(
                    recsQuery.copy()
                        .withSourceId(JournalsRegistryConfiguration.JOURNALS_REGISTRY_SOURCE_ID)
                        .build()
                )

                val journals = journalService.getAll(
                    queryRes.getRecords().map {
                        workspaceService.convertToIdInWs(it.getLocalId())
                    }.toSet()
                )
                result.setRecords(ArrayList(journals))
                result.setTotalCount(queryRes.getTotalCount())
            } else {
                result.setRecords(
                    ArrayList(
                        journalService.getAll(recsQuery.page.maxItems, recsQuery.page.skipCount)
                    )
                )
                result.setTotalCount(journalService.getCount())
            }
        }

        val res = RecsQueryRes<JournalRecord>()
        res.setTotalCount(result.getTotalCount())
        res.setHasMore(result.getHasMore())
        res.setRecords(result.getRecords().map { JournalRecord(it, workspaceService) })
        return res
    }

    override fun getRecordAtts(recordId: String): JournalRecord {
        val dto = if (recordId.isEmpty()) {
            JournalWithMeta(false)
        } else {
            val idInWs = workspaceService.convertToIdInWs(recordId)
            if (!workspaceService.isWorkspaceWithGlobalEntities(idInWs.workspace) &&
                !workspaceService.isUserMemberOf(AuthContext.getCurrentUser(), idInWs.workspace)
            ) {
                JournalWithMeta(false)
            } else {
                journalService.getJournalById(idInWs) ?: JournalWithMeta(false)
            }
        }
        return JournalRecord(dto, workspaceService)
    }

    override fun delete(recordId: String): DelStatus {
        val idInWs = workspaceService.convertToIdInWs(recordId)
        checkWritePermissions(idInWs.workspace)
        journalService.delete(idInWs)
        return DelStatus.OK
    }

    override fun getRecToMutate(recordId: String): JournalMutateRec {
        if (recordId.isEmpty()) {
            return JournalMutateRec(JournalDef.create().build(), workspaceService)
        }
        val idInWs = workspaceService.convertToIdInWs(recordId)
        val dto = journalService.getJournalById(idInWs) ?: error("Record with id '$idInWs' doesn't found")
        return JournalMutateRec(dto.journalDef, workspaceService)
    }

    override fun saveMutatedRec(record: JournalMutateRec): String {
        checkWritePermissions(record.workspace)

        var typeLocalId = record.typeRef.getLocalId()
        if (typeLocalId.isNotBlank()) {
            typeLocalId = workspaceService.replaceCurrentWsPlaceholderToWsPrefix(typeLocalId, record.workspace)
            record.withTypeRef(record.typeRef.withLocalId(typeLocalId))
        }

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

        val afterSave = journalService.save(record.build()).journalDef
        return workspaceService.addWsPrefixToId(afterSave.id, afterSave.workspace)
    }

    fun checkWritePermissions(workspace: String?) {
        if (workspaceService.getArtifactsWritePermission(AuthContext.getCurrentUser(), workspace, "journal")) {
            return
        }
        error("Permission denied. You can't create or change journals in workspace '$workspace'")
    }

    @Data
    class JournalQueryByTypeRef(
        val typeRef: EntityRef? = null
    )

    class ActionDefRecord(
        @param:AttName("...")
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

    open class JournalRecord(base: JournalWithMeta, val workspaceService: WorkspaceService) : JournalWithMeta(base) {

        @AttName(ScalarType.ID_SCHEMA)
        open fun getRef(): EntityRef {
            val journalId = journalDef?.id ?: ""
            val localId = if (journalId.startsWith("type$")) {
                journalId
            } else {
                workspaceService.addWsPrefixToId(journalId, journalDef?.workspace ?: "")
            }
            return EntityRef.create(AppName.UISERV, ID, localId)
        }

        open fun getModuleId(): String {
            return getLocalId()
        }

        open fun getLocalId(): String {
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

        @JsonValue
        open fun toNonDefaultJson(): Any? {
            val journalDef = journalDef ?: return null
            return mapper.toNonDefaultJson(journalDef.copy().withWorkspace("").build())
        }

        open fun getColumns(): List<Any> {
            return journalDef?.columns?.map { ColumnAttValue(it) } ?: emptyList()
        }

        open fun getData(): ByteArray? {
            val journalDef = journalDef ?: return null
            val journalDefCopy = journalDef.copy {
                withWorkspace("")

                var typeLocalId = journalDef.typeRef.getLocalId()
                if (typeLocalId.isNotBlank()) {
                    typeLocalId = workspaceService.replaceWsPrefixToCurrentWsPlaceholder(typeLocalId)
                    withTypeRef(journalDef.typeRef.withLocalId(typeLocalId))
                }
            }
            return YamlUtils.toNonDefaultString(journalDefCopy).toByteArray(StandardCharsets.UTF_8)
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
                        if (JournalServiceImpl.SYSTEM_JOURNALS.contains(journalId)) {
                            false
                        } else {
                            val workspace = journalDef?.workspace ?: ModelUtils.DEFAULT_WORKSPACE_ID
                            isRunAsAdmin() || workspaceService.isUserManagerOf(AuthContext.getCurrentUser(), workspace)
                        }
                    }
                } else {
                    name.equals("read", ignoreCase = true)
                }
            }
        }
    }

    class ColumnAttValue(
        @param:AttName("...")
        val column: JournalColumnDef
    ) {
        fun getJsonValue(): ObjectData {
            return ObjectData.create(column)
        }
    }

    class JournalMutateRec(
        base: JournalDef,
        private val workspaceService: WorkspaceService
    ) : JournalDef.Builder(base) {

        val originalId = base.id

        fun withModuleId(id: String) {
            withId(id)
        }

        @JsonProperty(RecordConstants.ATT_WORKSPACE)
        fun withCtxWorkspace(workspace: String) {
            if (originalId.isNotBlank() && originalId != id) {
                withWorkspace(workspace)
            } else {
                withWorkspace(workspaceService.getUpdatedWsInMutation(this.workspace, workspace))
            }
        }
    }
}
