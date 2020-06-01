package ru.citeck.ecos.uiserv.service.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.evaluator.evaluators.AlwaysFalseEvaluator;
import ru.citeck.ecos.records2.evaluator.evaluators.AlwaysTrueEvaluator;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.uiserv.domain.ActionEntity;
import ru.citeck.ecos.uiserv.domain.EvaluatorEntity;
import ru.citeck.ecos.uiserv.repository.ActionRepository;
import ru.citeck.ecos.uiserv.security.SecurityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionService {

    private final RecordEvaluatorService evaluatorsService;
    private final ActionRepository actionRepository;

    private Consumer<ActionModule> changeListener;

    private ObjectMapper objectMapper = new ObjectMapper();

    public ActionModule getAction(String id) {
        return entityToDto(actionRepository.findByExtId(id));
    }

    public int getCount() {
        return (int) actionRepository.count();
    }

    public List<ActionModule> getActions(int max, int skip) {

        PageRequest page = PageRequest.of(skip / max, max, Sort.by(Sort.Direction.DESC, "id"));

        return actionRepository.findAll(page)
            .stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }

    public void updateAction(ActionModule action) {
        ActionEntity actionEntity = dtoToEntity(action);
        actionEntity = actionRepository.save(actionEntity);
        changeListener.accept(entityToDto(actionEntity));
    }

    public void onActionChanged(Consumer<ActionModule> listener) {
        changeListener = listener;
    }

    public void deleteAction(String id) {
        ActionEntity action = actionRepository.findByExtId(id);
        if (action != null) {
            actionRepository.delete(action);
        }
    }

    private List<ActionModule> getActionModules(List<RecordRef> actionRefs) {

        List<ActionModule> result = new ArrayList<>();

        for (RecordRef ref : actionRefs) {
            ActionEntity entity = actionRepository.findByExtId(ref.getId());
            if (entity == null) {
                log.error("Action doesn't exists: " + ref);
            } else {
                result.add(entityToDto(entity));
            }
        }

        return result;
    }

    public Map<RecordRef, List<ActionModule>> getActions(List<RecordRef> recordRefs, List<RecordRef> actions) {

        List<ActionModule> actionsModules = getActionModules(actions);

        List<RecordEvaluatorDto> evaluators = actionsModules.stream()
            .map(a -> {
                RecordEvaluatorDto recordEvaluatorDto = a.getEvaluator();
                if (recordEvaluatorDto == null) {
                    recordEvaluatorDto = new RecordEvaluatorDto();
                    recordEvaluatorDto.setType(AlwaysTrueEvaluator.TYPE);
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

        String requestUsername = SecurityUtils.getCurrentUserLoginFromRequestContext();
        model.put("user", RecordRef.valueOf("alfresco/people@" + requestUsername));
        model.put("alfMeta", RecordRef.valueOf("alfresco/meta@"));

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
        action.setName(Json.getMapper().read(actionEntity.getName(), MLText.class));
        action.setKey(actionEntity.getKey());
        action.setType(actionEntity.getType());
        action.setConfirm(Json.getMapper().read(actionEntity.getName(), ActionConfirmDto.class));
        action.setResult(Json.getMapper().read(actionEntity.getName(), ActionResultDto.class));

        String configJson = actionEntity.getConfigJson();
        if (configJson != null) {
            action.setConfig(Json.getMapper().convert(configJson, ObjectData.class));
        }

        EvaluatorEntity evaluator = actionEntity.getEvaluator();
        RecordEvaluatorDto evaluatorDto = null;

        if (evaluator != null) {

            evaluatorDto = new RecordEvaluatorDto();
            evaluatorDto.setId(evaluator.getEvaluatorId());
            evaluatorDto.setType(evaluator.getType());
            evaluatorDto.setInverse(evaluator.isInverse());

            configJson = evaluator.getConfigJson();
            if (configJson != null) {
                evaluatorDto.setConfig(Json.getMapper().convert(configJson, ObjectData.class));
            }
        }

        if (isValidEvaluator(evaluatorDto)) {
            action.setEvaluator(evaluatorDto);
        } else {
            action.setEvaluator(null);
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
        actionEntity.setName(Json.getMapper().toString(action.getName()));
        actionEntity.setType(action.getType());
        actionEntity.setConfirm(Json.getMapper().toString(action.getConfirm()));
        actionEntity.setResult(Json.getMapper().toString(action.getResult()));

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
        if (isValidEvaluator(evaluator)) {

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

        } else {

            actionEntity.setEvaluator(null);
        }

        return actionEntity;
    }

    private boolean isValidEvaluator(RecordEvaluatorDto ev) {
        return ev != null && (StringUtils.isNotBlank(ev.getId()) || StringUtils.isNotBlank(ev.getType()));
    }
}
