package ru.citeck.ecos.uiserv.domain.action.service

import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.uiserv.domain.action.dao.ActionDao
import ru.citeck.ecos.uiserv.domain.action.dto.ActionConfirmDef
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.dto.ActionResultDto
import ru.citeck.ecos.uiserv.domain.action.repo.ActionEntity
import ru.citeck.ecos.uiserv.domain.evaluator.repo.EvaluatorEntity

@Component
class ActionEntityMapper(
    private val actionDao: ActionDao
) {

    fun toDto(actionEntity: ActionEntity?): ActionDto? {

        actionEntity ?: return null

        val action = ActionDto()

        action.id = actionEntity.extId
        action.icon = actionEntity.icon
        action.name = mapper.read(actionEntity.name, MLText::class.java)
        action.pluralName = mapper.read(actionEntity.pluralName, MLText::class.java)
        action.preActionModule = actionEntity.preActionModule
        action.type = actionEntity.type
        action.confirm = mapper.read(actionEntity.confirm, ActionConfirmDef::class.java)
        action.result = mapper.read(actionEntity.result, ActionResultDto::class.java)

        val features = mapper.read(actionEntity.features, DataValue::class.java)
        if (features != null && features.isNotNull()) {
            action.features = features.asMap(String::class.java, Boolean::class.java)
        }
        var configJson = actionEntity.configJson
        if (configJson != null) {
            action.config = mapper.convert(configJson, ObjectData::class.java)
        }
        val evaluator = actionEntity.evaluator
        var evaluatorDto: RecordEvaluatorDto? = null
        if (evaluator != null) {
            evaluatorDto = RecordEvaluatorDto()
            evaluatorDto.id = evaluator.evaluatorId
            evaluatorDto.type = evaluator.type
            evaluatorDto.isInverse = evaluator.inverse
            configJson = evaluator.configJson
            if (configJson != null) {
                evaluatorDto.config = mapper.convert(configJson, ObjectData::class.java)
            }
        }
        action.predicate = mapper.read(actionEntity.predicate, Predicate::class.java)

        if (isValidEvaluator(evaluatorDto)) {
            action.evaluator = evaluatorDto
        } else {
            action.evaluator = null
        }

        return action
    }

    fun toEntity(action: ActionDto): ActionEntity {

        var actionEntity = actionDao.getAction(action.id)
        if (actionEntity == null) {
            actionEntity = ActionEntity()
            actionEntity.extId = action.id
        }
        actionEntity.icon = action.icon
        actionEntity.name = mapper.toString(action.name)
        actionEntity.pluralName = mapper.toString(action.pluralName)
        actionEntity.preActionModule = action.preActionModule
        actionEntity.type = action.type
        actionEntity.confirm = mapper.toString(action.confirm)
        actionEntity.result = mapper.toString(action.result)
        actionEntity.features = mapper.toString(action.features)
        actionEntity.configJson = mapper.toString(action.config)
        actionEntity.predicate = mapper.toString(action.predicate)

        val evaluator = action.evaluator
        if (isValidEvaluator(evaluator)) {
            var evaluatorEntity = actionEntity.evaluator
            if (evaluatorEntity == null) {
                evaluatorEntity = EvaluatorEntity()
            }
            evaluatorEntity.configJson = mapper.toString(evaluator.config)
            evaluatorEntity.evaluatorId = evaluator.id
            evaluatorEntity.type = evaluator.type
            evaluatorEntity.inverse = evaluator.isInverse
            actionEntity.evaluator = evaluatorEntity
        } else {
            actionEntity.evaluator = null
        }
        return actionEntity
    }

    private fun isValidEvaluator(ev: RecordEvaluatorDto?): Boolean {
        return ev != null && (StringUtils.isNotBlank(ev.id) || StringUtils.isNotBlank(ev.type))
    }
}
