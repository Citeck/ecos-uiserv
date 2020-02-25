package ru.citeck.ecos.uiserv.service.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.apps.app.module.ModuleRef;
import ru.citeck.ecos.apps.app.module.type.ui.action.ActionModule;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.evaluator.evaluators.AlwaysFalseEvaluator;
import ru.citeck.ecos.records2.evaluator.evaluators.AlwaysTrueEvaluator;
import ru.citeck.ecos.records2.objdata.ObjectData;
import ru.citeck.ecos.records2.utils.json.JsonUtils;
import ru.citeck.ecos.uiserv.domain.ActionEntity;
import ru.citeck.ecos.uiserv.domain.EvaluatorEntity;
import ru.citeck.ecos.uiserv.repository.ActionRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionService {

    private final RecordEvaluatorService evaluatorsService;
    private final ActionRepository actionRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    public void updateAction(ActionModule action) {
        ActionEntity actionEntity = dtoToEntity(action);
        actionRepository.save(actionEntity);
    }

    public void deleteAction(String id) {
        ActionEntity action = actionRepository.findByExtId(id);
        if (action != null) {
            actionRepository.delete(action);
        }
    }

    private List<ActionModule> getActionModules(List<ModuleRef> actionRefs) {

        List<ActionModule> result = new ArrayList<>();

        for (ModuleRef ref : actionRefs) {
            ActionEntity entity = actionRepository.findByExtId(ref.getId());
            if (entity == null) {
                log.error("Action doesn't exists: " + ref);
            } else {
                result.add(entityToDto(entity));
            }
        }

        return result;
    }

    public Map<RecordRef, List<ActionModule>> getActions(List<RecordRef> recordRefs, List<ModuleRef> actions) {

        List<ActionModule> actionsModules = getActionModules(actions);

        List<RecordEvaluatorDto> evaluators = actionsModules.stream()
            .map(a -> {
                RecordEvaluatorDto recordEvaluatorDto;
                if (a.getEvaluator() == null) {
                    recordEvaluatorDto = new RecordEvaluatorDto();
                    recordEvaluatorDto.setType(AlwaysTrueEvaluator.TYPE);
                } else {
                    recordEvaluatorDto = a.getEvaluator();
                }
                if (recordEvaluatorDto.getType() == null) {
                    recordEvaluatorDto.setType(recordEvaluatorDto.getId());
                }
                if (recordEvaluatorDto.getType() == null) {
                    log.error("Evaluator type is null: '" + recordEvaluatorDto + "'. " +
                        "Replace it with Always False Evaluator. Action: " + a);
                    recordEvaluatorDto.setType(AlwaysFalseEvaluator.TYPE);
                }
                return recordEvaluatorDto;
            }).collect(Collectors.toList());

        Map<String, RecordRef> model = new HashMap<>();
        model.put("user", RecordRef.valueOf("alfresco/people@${currentUsername}"));

        Map<RecordRef, List<Boolean>> evalResultByRecord = evaluatorsService.evaluate(recordRefs, evaluators, model);
        Map<RecordRef, List<ActionModule>> actionsByRecord = new HashMap<>();

        for (RecordRef recordRef : recordRefs) {

            List<Boolean> evalResult = evalResultByRecord.get(recordRef);
            List<ActionModule> recordActions = new ArrayList<>();

            for (int j = 0; j < actionsModules.size(); j++) {
                if (evalResult.get(j)) {
                    recordActions.add(actionsModules.get(j));
                }
            }

            actionsByRecord.put(recordRef, recordActions);
        }

        return actionsByRecord;
    }

    private ActionModule entityToDto(ActionEntity actionEntity) {

        ActionModule action = new ActionModule();
        action.setId(actionEntity.getExtId());
        action.setIcon(actionEntity.getIcon());
        action.setName(actionEntity.getName());
        action.setKey(actionEntity.getKey());
        action.setType(actionEntity.getType());

        String configJson = actionEntity.getConfigJson();
        if (configJson != null) {
            action.setConfig(JsonUtils.convert(configJson, ObjectData.class));
        }

        EvaluatorEntity evaluator = actionEntity.getEvaluator();
        if (evaluator != null) {
            RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
            evaluatorDto.setId(evaluator.getEvaluatorId());
            evaluatorDto.setType(evaluator.getType());
            evaluatorDto.setInverse(evaluator.isInverse());

            configJson = evaluator.getConfigJson();
            if (configJson != null) {
                evaluatorDto.setConfig(JsonUtils.convert(configJson, ObjectData.class));
            }

            action.setEvaluator(evaluatorDto);
        }

        return action;
    }

    private ActionEntity dtoToEntity(ActionModule action) {

        ActionEntity actionEntity = actionRepository.findByExtId(action.getId());
        if (actionEntity == null) {
            actionEntity = new ActionEntity();
            actionEntity.setExtId(action.getId());
        }

        actionEntity.setIcon(action.getIcon());
        actionEntity.setKey(action.getKey());
        actionEntity.setName(action.getName());
        actionEntity.setType(action.getType());

        if (action.getConfig() != null) {
            try {
                actionEntity.setConfigJson(objectMapper.writeValueAsString(action.getConfig()));
            } catch (JsonProcessingException e) {
                log.error("Error", e);
                actionEntity.setConfigJson(null);
            }
        } else {
            actionEntity.setConfigJson(null);
        }

        RecordEvaluatorDto evaluator = action.getEvaluator();
        if (evaluator != null) {

            EvaluatorEntity evaluatorEntity = actionEntity.getEvaluator();
            if (evaluatorEntity == null) {
                evaluatorEntity = new EvaluatorEntity();
            }
            if (evaluator.getConfig() != null) {
                try {
                    evaluatorEntity.setConfigJson(objectMapper.writeValueAsString(evaluator.getConfig()));
                } catch (JsonProcessingException e) {
                    log.error("Error", e);
                    evaluatorEntity.setConfigJson(null);
                }
            } else {
                evaluatorEntity.setConfigJson(null);
            }

            evaluatorEntity.setEvaluatorId(evaluator.getId());
            evaluatorEntity.setType(evaluator.getType());
            evaluatorEntity.setInverse(evaluator.isInverse());

            actionEntity.setEvaluator(evaluatorEntity);
        }

        return actionEntity;
    }
}
