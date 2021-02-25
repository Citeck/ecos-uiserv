package ru.citeck.ecos.uiserv.domain.action.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService
import ru.citeck.ecos.records2.evaluator.evaluators.AlwaysFalseEvaluator
import ru.citeck.ecos.records2.evaluator.evaluators.AlwaysTrueEvaluator
import ru.citeck.ecos.records2.evaluator.evaluators.PredicateEvaluator
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.dto.RecordsActionsDto
import ru.citeck.ecos.uiserv.domain.action.repo.ActionRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

@Service
class ActionService(
    private val evaluatorsService: RecordEvaluatorService,
    private val actionRepository: ActionRepository,
    private val actionEntityMapper: ActionEntityMapper
) {
    companion object {
        val log = KotlinLogging.logger {}
    }

    private val actionProviders: MutableMap<String, ActionsProvider> = ConcurrentHashMap()
    private var changeListener: Consumer<ActionDto>? = null

    fun getAction(id: String): ActionDto? {

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

    fun getCount(): Long {
        return actionRepository.count()
    }

    fun getActions(max: Int, skip: Int): List<ActionDto> {
        if (max == 0) {
            return emptyList()
        }
        val page = PageRequest.of(skip / max, max, Sort.by(Sort.Direction.DESC, "id"))

        return actionRepository.findAll(page).mapNotNull { actionEntityMapper.toDto(it) }
    }

    fun updateAction(action: ActionDto) {
        var actionEntity = actionEntityMapper.toEntity(action)
        actionEntity = actionRepository.save(actionEntity)
        changeListener?.accept(actionEntityMapper.toDto(actionEntity)!!)
    }

    fun onActionChanged(listener: Consumer<ActionDto>?) {
        changeListener = listener
    }

    fun deleteAction(id: String?) {
        val action = actionRepository.findByExtId(id)
        if (action != null) {
            actionRepository.delete(action)
        }
    }

    private fun getActionArtifacts(actionRefs: List<RecordRef>): List<ActionDto> {
        val result: MutableList<ActionDto> = ArrayList()
        for (ref in actionRefs) {
            val actionDto = getAction(ref.id)
            if (actionDto == null) {
                log.error("Action doesn't exists: $ref")
            } else {
                result.add(actionDto)
            }
        }
        return result
    }

    fun getActions(recordRefs: List<RecordRef>, actions: List<RecordRef>): Map<RecordRef, List<ActionDto>> {

        val actionsForRecords = getActionsForRecords(recordRefs, actions)
        val result: MutableMap<RecordRef, List<ActionDto>> = HashMap()
        val actionById: MutableMap<String, ActionDto> = HashMap()

        actionsForRecords.actions.forEach(Consumer { a: ActionDto -> actionById[a.id] = a })
        actionsForRecords.recordActions.forEach { (recordRef: RecordRef, refActions: Set<String>) ->
            result[recordRef] = refActions.mapNotNull { actionById[it] }
        }

        return result
    }

    fun getActionsForRecords(recordRefs: List<RecordRef>, actions: List<RecordRef>): RecordsActionsDto {

        val actionArtifacts = getActionArtifacts(actions)

        val evaluators = actionArtifacts.map { actionDto ->
            var recordEvaluatorDto = actionDto.evaluator

            if (recordEvaluatorDto == null && actionDto.predicate != null
                && actionDto.predicate != VoidPredicate.INSTANCE) {

                recordEvaluatorDto = RecordEvaluatorDto()
                recordEvaluatorDto.type = PredicateEvaluator.TYPE

                val config = ObjectData.create()
                config.set("predicate", actionDto.predicate)
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
                log.error("Evaluator type is null: '" + recordEvaluatorDto + "'. " +
                    "Replace it with Always False Evaluator. Action: " + actionDto)
                recordEvaluatorDto.type = AlwaysFalseEvaluator.TYPE
            }
            recordEvaluatorDto
        }

        val evalResultByRecord = evaluatorsService.evaluate(recordRefs, evaluators)
        val recordActionsByRef: MutableMap<RecordRef, Set<String>> = HashMap()

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

    @Autowired(required = false)
    fun setActionProviders(actionProviders: List<ActionsProvider>) {
        actionProviders.forEach(Consumer { prov: ActionsProvider -> this.actionProviders[prov.getType()] = prov })
    }
}