package ru.citeck.ecos.uiserv.domain.action.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.app.common.perms.UiServSystemArtifactPerms
import ru.citeck.ecos.uiserv.domain.action.api.records.ActionRecords
import ru.citeck.ecos.uiserv.domain.action.dao.ActionDao
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.dto.RecordsActionsDto
import ru.citeck.ecos.uiserv.domain.action.repo.ActionEntity
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorDto
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorService
import ru.citeck.ecos.uiserv.domain.evaluator.evaluators.AlwaysFalseEvaluator
import ru.citeck.ecos.uiserv.domain.evaluator.evaluators.AlwaysTrueEvaluator
import ru.citeck.ecos.uiserv.domain.evaluator.evaluators.PredicateEvaluator
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

@Service
class ActionService(
    private val evaluatorsService: RecordEvaluatorService,
    private val actionEntityMapper: ActionEntityMapper,
    private val actionDao: ActionDao,
    private val perms: UiServSystemArtifactPerms,
    private val workspaceService: WorkspaceService
) {
    companion object {
        val log = KotlinLogging.logger {}

        private const val ARTIFACT_TYPE_ID = "action"
    }

    private val actionProviders: MutableMap<String, ActionsProvider> = ConcurrentHashMap()
    private var changeListeners: MutableList<(ActionDto?, ActionDto) -> Unit> = CopyOnWriteArrayList()

    /**
     * Resolve action by the local id of its ref: 'id' for a global action,
     * '<wsSysId>:id' for an action in workspace or '<provider>$id' for provided actions.
     * The returned dto has a bare id and the workspace in [ActionDto.workspace].
     */
    fun getAction(id: String): ActionDto? {

        val idInWs = workspaceService.convertToIdInWs(id)
        if (idInWs.workspace.isNotEmpty()) {
            return getAction(idInWs)
        }

        val providerDelimIdx = id.indexOf('$')

        var providerId = ""
        var localId = id
        if (providerDelimIdx > 0 && providerDelimIdx < id.length - 1) {
            providerId = id.substring(0, providerDelimIdx)
            localId = id.substring(providerDelimIdx + 1)
        }

        var provider = actionProviders[providerId]
        if (provider == null && providerId.isNotEmpty()) {
            provider = actionProviders[""]
            localId = id
        }
        if (provider == null) {
            log.error { "Provider is not found: '$providerId'" }
            return null
        }

        return provider.getAction(localId)
    }

    /**
     * An action in workspace is available only for members of the workspace, admins and system.
     * Unavailable action is resolved as absent.
     */
    fun getAction(id: IdInWs): ActionDto? {
        val workspace = actionEntityMapper.toStorageWorkspace(id.workspace)
        if (workspace.isEmpty()) {
            return getAction(id.id)
        }
        if (!isReadAllowed(workspace)) {
            log.debug { "Action '$id' is not available for user '${AuthContext.getCurrentUser()}'" }
            return null
        }
        return actionEntityMapper.toDto(actionDao.getAction(id.id, workspace))
    }

    private fun isReadAllowed(workspace: String): Boolean {
        return AuthContext.isRunAsSystemOrAdmin() ||
            workspaceService.isRunAsSystemOrWsSystem(workspace) ||
            workspaceService.isUserMemberOf(AuthContext.getCurrentUser(), workspace)
    }

    /**
     * Local id of the action ref: bare id for a global action and '<wsSysId>:id' for an action in workspace.
     */
    fun getRefLocalId(action: ActionDto): String {
        val id: String = action.id ?: ""
        if (id.isEmpty()) {
            return id
        }
        return workspaceService.addWsPrefixToId(id, action.workspace ?: "")
    }

    fun getCount(predicate: Predicate, workspaces: List<String>): Long {
        val fullPredicate = withWorkspacesCondition(predicate, workspaces) ?: return 0
        return actionDao.getCount(fullPredicate)
    }

    fun getActions(
        predicate: Predicate,
        workspaces: List<String>,
        max: Int,
        skip: Int,
        sort: List<SortBy>
    ): List<ActionDto> {
        val fullPredicate = withWorkspacesCondition(predicate, workspaces) ?: return emptyList()
        return getActionEntities(fullPredicate, max, skip, sort).mapNotNull { actionEntityMapper.toDto(it) }
    }

    /**
     * Restrict the query to workspaces available for the current user.
     * Null means that no workspace is available and the result is empty.
     */
    private fun withWorkspacesCondition(predicate: Predicate, workspaces: List<String>): Predicate? {
        val wsPredicate = workspaceService.buildAvailableWorkspacesPredicate(
            AuthContext.getCurrentRunAsAuth(),
            workspaces
        )
        return if (PredicateUtils.isAlwaysFalse(wsPredicate)) {
            null
        } else if (PredicateUtils.isAlwaysTrue(wsPredicate)) {
            predicate
        } else {
            Predicates.and(predicate, wsPredicate)
        }
    }

    fun updateAction(action: ActionDto) {

        val workspace = actionEntityMapper.toStorageWorkspace(action.workspace)
        checkWrite(IdInWs.create(workspace, action.id))

        val before = actionDao.getAction(action.id, workspace)?.let { actionEntityMapper.toDto(it) }

        var actionEntity = actionEntityMapper.toEntity(action)
        actionEntity = actionDao.save(actionEntity)
        val after = actionEntityMapper.toDto(actionEntity)!!

        for (listener in changeListeners) {
            listener(before, after)
        }
    }

    fun onActionChanged(action: (ActionDto?, ActionDto) -> Unit) {
        changeListeners.add(action)
    }

    /**
     * @param id local id of the action ref ('id' or '<wsSysId>:id')
     */
    fun deleteAction(id: String?) {
        id ?: return
        deleteAction(workspaceService.convertToIdInWs(id))
    }

    fun deleteAction(id: IdInWs) {

        val workspace = actionEntityMapper.toStorageWorkspace(id.workspace)
        checkWrite(IdInWs.create(workspace, id.id))

        val action = actionDao.getAction(id.id, workspace)
        if (action != null) {
            actionDao.delete(action)
        }
    }

    /**
     * Global actions are system artifacts and may be changed by admins only.
     * Actions in workspace may be changed by the workspace managers too.
     */
    private fun checkWrite(id: IdInWs) {
        if (id.workspace.isEmpty()) {
            perms.checkWrite(EntityRef.create(AppName.UISERV, ActionRecords.ID, id.id))
            return
        }
        val user = AuthContext.getCurrentUser()
        if (!workspaceService.getArtifactsWritePermission(user, id.workspace, ARTIFACT_TYPE_ID)) {
            throw IllegalAccessException(
                "Permission denied. You can't create or change actions in workspace '${id.workspace}'"
            )
        }
    }

    private fun getActionArtifacts(actionRefs: List<EntityRef>): List<ActionDto> {
        val result: MutableList<ActionDto> = ArrayList()
        for (ref in actionRefs) {
            val actionDto = getAction(ref.getLocalId())
            if (actionDto == null) {
                log.error { "Action doesn't exists or isn't available: $ref" }
            } else if (actionDto.workspace.isNotEmpty()) {
                // ids in the result are matched with requested refs
                // and must not collide with global actions or actions from other workspaces
                val actionWithRefId = ActionDto(actionDto)
                actionWithRefId.id = getRefLocalId(actionDto)
                result.add(actionWithRefId)
            } else {
                result.add(actionDto)
            }
        }
        return result
    }

    fun getActions(recordRefs: List<EntityRef>, actions: List<EntityRef>): Map<EntityRef, List<ActionDto>> {

        val actionsForRecords = getActionsForRecords(recordRefs, actions)
        val result: MutableMap<EntityRef, List<ActionDto>> = HashMap()
        val actionById: MutableMap<String, ActionDto> = HashMap()

        actionsForRecords.actions.forEach(Consumer { a: ActionDto -> actionById[a.id] = a })
        actionsForRecords.recordActions.forEach { (recordRef: EntityRef, refActions: Set<String>) ->
            result[recordRef] = refActions.mapNotNull { actionById[it] }
        }

        return result
    }

    fun getActionsForRecords(recordRefs: List<EntityRef>, actions: List<EntityRef>): RecordsActionsDto {

        val actionArtifacts = getActionArtifacts(actions)

        val evaluators = actionArtifacts.map { actionDto ->
            var recordEvaluatorDto = actionDto.evaluator

            if (recordEvaluatorDto == null &&
                actionDto.predicate != null &&
                actionDto.predicate != VoidPredicate.INSTANCE
            ) {

                recordEvaluatorDto = RecordEvaluatorDto()
                recordEvaluatorDto.type = PredicateEvaluator.TYPE

                val config = ObjectData.create()
                config["predicate"] = actionDto.predicate
                recordEvaluatorDto.config = config
            }

            if (recordEvaluatorDto == null) {
                recordEvaluatorDto = RecordEvaluatorDto()
                recordEvaluatorDto.type = AlwaysTrueEvaluator.TYPE
            }
            if (recordEvaluatorDto.type == null) {
                recordEvaluatorDto.type = recordEvaluatorDto.id
            }
            if (recordEvaluatorDto.type == null) {
                log.error {
                    "Evaluator type is null: '" + recordEvaluatorDto + "'. " +
                        "Replace it with Always False Evaluator. Action: " + actionDto
                }
                recordEvaluatorDto.type = AlwaysFalseEvaluator.TYPE
            }
            recordEvaluatorDto
        }

        val evalResultByRecord = evaluatorsService.evaluate(recordRefs, evaluators)
        val recordActionsByRef: MutableMap<EntityRef, Set<String>> = HashMap()

        for (recordRef in recordRefs) {
            val evalResult = evalResultByRecord[recordRef] ?: emptyList()
            val recordActions: MutableSet<String> = HashSet()
            for (j in actionArtifacts.indices) {
                if (evalResult[j]) {
                    recordActions.add(actionArtifacts[j].id)
                }
            }
            recordActionsByRef[recordRef] = recordActions
        }

        val recordsActions = RecordsActionsDto()
        recordsActions.recordActions = recordActionsByRef
        recordsActions.actions = actionArtifacts

        return recordsActions
    }

    private fun getActionEntities(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<ActionEntity> {
        return actionDao.getActions(predicate, max, skip, sort)
    }

    fun addActionProvider(provider: ActionsProvider) {
        this.actionProviders[provider.getType()] = provider
    }

    @Autowired(required = false)
    fun setActionProviders(actionProviders: List<ActionsProvider>) {
        actionProviders.forEach(Consumer { prov: ActionsProvider -> this.actionProviders[prov.getType()] = prov })
    }
}
