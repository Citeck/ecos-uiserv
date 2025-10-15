package ru.citeck.ecos.uiserv.domain.form.api.records

import jakarta.annotation.PostConstruct
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.type.RecordEventsService
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.AttributePredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.uiserv.domain.form.registry.FormsRegistryConfiguration
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*
import java.util.stream.Collectors

@Component
class EcosFormRecordsDao(
    private val ecosFormService: EcosFormService,
    private var recordEventsService: RecordEventsService?,
    private val workspaceService: WorkspaceService
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordMutateDtoDao<EcosFormMutRecord>,
    RecordDeleteDao,
    RecordAttsDao {

    companion object {

        const val ID = "form"

        private const val QUERY_LANG_FORMS_FOR_TYPE = "forms-for-type"
        private const val QUERY_LANG_FORMS_FOR_MOBILE_TASKS = "forms-for-mobile-tasks"

        // form with default content for new forms
        private const val DEFAULT_FORM_ID = "DEFAULT"

        // form to create and edit any form metadata and definition
        private const val ECOS_FORM_ID = "ECOS_FORM"
        private const val DEFAULT_AUTO_FORM_FOR_TYPE = "DEFAULT_FORM"

        private val ATTS_MAPPING: MutableMap<String, String>

        val SYSTEM_FORMS: Set<String> = setOf(
            DEFAULT_AUTO_FORM_FOR_TYPE,
            DEFAULT_FORM_ID,
            ECOS_FORM_ID
        )

        init {
            ATTS_MAPPING = HashMap()
            ATTS_MAPPING["moduleId"] = "id"
        }
    }

    @PostConstruct
    fun init() {
        ecosFormService.addChangeListener { before: EcosFormDef?, after: EcosFormDef? ->
            if (after != null) {
                recordEventsService?.emitRecChanged(before, after, getId()) {
                    EcosFormRecord(AppName.UISERV, ID, it, workspaceService)
                }
            }
        }
    }

    override fun getRecToMutate(recordId: String): EcosFormMutRecord {
        if (recordId.isEmpty()) {
            return EcosFormMutRecord(workspaceService)
        }
        val idInWs = workspaceService.convertToIdInWs(recordId)
        val currentForm = ecosFormService.getFormById(idInWs)
        require(!currentForm.isEmpty) { "Form with id $recordId not found!" }
        return EcosFormMutRecord(currentForm.get(), workspaceService)
    }

    private fun toRecord(model: EcosFormDef): EcosFormRecord {
        return EcosFormRecord(AppName.UISERV, ID, model, workspaceService)
    }

    override fun saveMutatedRec(record: EcosFormMutRecord): String {
        checkPermissions(record.workspace)
        val recAfterSave = ecosFormService.save(record.build())
        return workspaceService.addWsPrefixToId(recAfterSave.id, recAfterSave.workspace)
    }

    override fun delete(recordId: String): DelStatus {
        if (SYSTEM_FORMS.contains(recordId)) {
            return DelStatus.PROTECTED
        }
        val idInWs = workspaceService.convertToIdInWs(recordId)
        checkPermissions(idInWs.workspace)

        ecosFormService.delete(idInWs)
        return DelStatus.OK
    }

    private fun checkPermissions(workspace: String) {
        if (workspaceService.getArtifactsWritePermission(AuthContext.getCurrentUser(), workspace, "form")) {
            return
        }
        error("Permission denied. You can't manage forms in workspace '$workspace'")
    }

    override fun getRecordAtts(recordId: String): EcosFormRecord? {
        return if (recordId.isEmpty()) {
            toRecord(EcosFormDef.create().build())
        } else {
            val idInWs = workspaceService.convertToIdInWs(recordId)
            if (!workspaceService.isWorkspaceWithGlobalEntities(idInWs.workspace) &&
                !workspaceService.isUserMemberOf(AuthContext.getCurrentUser(), idInWs.workspace)
            ) {
                return null
            }
            ecosFormService.getFormById(idInWs)
                .map { toRecord(it) }
                .orElse(null)
        }
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<Any>? {

        val result = RecsQueryRes<Any>()
        var query: Query? = null

        if (StringUtils.isBlank(recsQuery.language)) {

            query = recsQuery.getQuery(Query::class.java)
        } else if (recsQuery.language == QUERY_LANG_FORMS_FOR_TYPE) {

            val formsForTypeQuery = recsQuery.getQuery(FormsForTypeQuery::class.java)
            if (EntityRef.isEmpty(formsForTypeQuery.typeRef)) {
                return result
            }
            result.addRecords(
                ecosFormService.getAllFormsForType(formsForTypeQuery.typeRef)
                    .map { toRecord(it) }
            )
            return result
        } else if (recsQuery.language == QUERY_LANG_FORMS_FOR_MOBILE_TASKS) {

            val forms = getFormsForMobileTasks(recsQuery.getQuery(FormsForMobileTasksQuery::class.java))
            result.setRecords(forms)
            return result
        }
        if (query == null) {
            val (max, skipCount) = recsQuery.page
            if (PredicateService.LANGUAGE_PREDICATE == recsQuery.language) {
                val predicate = PredicateUtils.mapAttributePredicates(
                    recsQuery.getQuery(Predicate::class.java)
                ) { pred: AttributePredicate ->
                    if (ATTS_MAPPING.containsKey(pred.getAttribute())) {
                        val copy = pred.copy<AttributePredicate>()
                        copy.setAttribute(ATTS_MAPPING[pred.getAttribute()])
                        copy
                    } else {
                        pred
                    }
                }
                val mappedSortBy = recsQuery.sortBy.map { sortBy: SortBy ->
                    if (ATTS_MAPPING.containsKey(sortBy.attribute)) {
                        sortBy.copy { this.attribute = ATTS_MAPPING[sortBy.attribute]!! }
                    } else {
                        sortBy
                    }
                }
                val registryQuery = recsQuery.copy()
                    .withSourceId(FormsRegistryConfiguration.FORMS_REGISTRY_SOURCE_ID)
                    .withQuery(predicate)
                    .withSortBy(mappedSortBy)
                    .build()
                val queryRes = recordsService.query(registryQuery)
                val journals = ecosFormService.getFormsByIds(
                    queryRes.getRecords().map {
                        workspaceService.convertToIdInWs(it.getLocalId())
                    }
                )
                result.setRecords(journals.map { toRecord(it.entity) })
                result.setTotalCount(queryRes.getTotalCount())
            } else {

                val forms = ecosFormService.getAllForms(
                    VoidPredicate.INSTANCE,
                    emptyList(),
                    max,
                    skipCount,
                    recsQuery.sortBy
                ).map { toRecord(it) }

                result.setRecords(forms)
                result.setTotalCount(ecosFormService.getCount(VoidPredicate.INSTANCE).toLong())
            }
            return result
        }
        var form = Optional.empty<EcosFormDef>()
        if (CollectionUtils.isNotEmpty(query.formKeys)) {

            val formsByKeys = ecosFormService.getFormsByKeys(query.formKeys)
                .stream()
                .map { model: EcosFormDef -> toRecord(model) }
                .collect(Collectors.toList())
            result.setTotalCount(formsByKeys.size.toLong())
            result.setRecords(formsByKeys)

            return result
        } else if (!query.formKey.isNullOrBlank()) {

            val keys = (query.formKey ?: "")
                .split(",")
                .filter { it.isNotBlank() }
            form = ecosFormService.getFormByKey(keys)
        }
        form.map { toRecord(it) }
            .map { listOf(it) }
            .ifPresent { list: List<EcosFormRecord> ->
                result.setRecords(list)
                result.setTotalCount(list.size.toLong())
            }
        return result
    }

    private fun getFormsForMobileTasks(query: FormsForMobileTasksQuery): List<EntityRef> {

        return query.tasks.map { taskRef ->
            val taskLocalId = taskRef.getLocalId()

            if (taskLocalId.startsWith("activiti$") || taskLocalId.startsWith("flowable$")) {
                val keys: List<String> = recordsService.getAtt(taskRef, "_formKey_mobile[]").asStrList()
                ecosFormService.getFormByKey(keys)
                    .orElse(EcosFormDef.EMPTY)
                    .let { EntityRef.create(AppName.UISERV, ID, it.id) }
            } else {
                EntityRef.create(AppName.UISERV, ID, "mobile-task$$taskRef")
            }
        }
    }

    override fun getId(): String {
        return ID
    }

    @Autowired(required = false)
    fun setRecordEventsService(recordEventsService: RecordEventsService?) {
        this.recordEventsService = recordEventsService
    }

    data class Query(
        val formKey: String? = null,
        val formKeys: List<String?>? = null,
        val record: EntityRef? = null,
        val isViewMode: Boolean? = null
    )

    data class FormsForTypeQuery(
        val typeRef: EntityRef? = null
    )

    data class FormsForMobileTasksQuery(
        val tasks: List<EntityRef>
    )
}
