package ru.citeck.ecos.uiserv.service.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.apps.app.module.type.ui.action.EvaluatorDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Slf4j
@Service
public class RecordEvaluatorService {

    private Map<String, RecordEvaluator> evaluators = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecordEvaluatorService(List<RecordEvaluator> evaluators) {
        evaluators.forEach(e -> this.evaluators.put(e.getId(), e));
    }

    public List<Map<String, String>> getMetaAttributes(List<EvaluatorDto> evaluators) {
        return evaluators.stream()
            .map(this::getMetaAttributes)
            .collect(Collectors.toList());
    }

    private Map<String, String> getMetaAttributes(EvaluatorDto evalDto) {

        @SuppressWarnings("unchecked")
        RecordEvaluator<Object, Object> evaluator =
            (RecordEvaluator<Object, Object>) this.evaluators.get(evalDto.getId());

        if (evaluator == null) {
            log.error("Evaluator with id " + evalDto.getId() + " is not found!");
            return Collections.emptyMap();
        }

        Map<String, String> attributes = null;
        try {
            Object convertedConfig = getConvertedValue(evalDto.getConfig(), evaluator.getConfigType());
            attributes = evaluator.getMetaAttributes(convertedConfig);
        } catch (Exception e) {
            log.error("Meta attributes can't be received. " +
                      "Id: " + evalDto.getId() + " Config: " + evalDto.getConfig(), e);
        }

        if (attributes == null) {
            attributes = Collections.emptyMap();
        }

        return attributes;
    }

    public List<Boolean> evaluate(List<EvaluatorDto> evaluators, List<ObjectNode> meta) {
        List<Boolean> result = new ArrayList<>();
        for (int i = 0; i < evaluators.size(); i++) {
            result.add(evaluate(evaluators.get(i), meta.get(i)));
        }
        return result;
    }

    private boolean evaluate(EvaluatorDto evalDto, ObjectNode metaNode) {

        @SuppressWarnings("unchecked")
        RecordEvaluator<Object, Object> evaluator =
            (RecordEvaluator<Object, Object>) this.evaluators.get(evalDto.getId());

        if (evaluator == null) {
            return false;
        }

        Object config = getConvertedValue(evalDto.getConfig(), evaluator.getConfigType());
        Object meta = getConvertedValue(metaNode, evaluator.getMetaType());

        try {
            boolean result = evaluator.evaluate(config, meta);
            return evalDto.isInverse() != result;
        } catch (Exception e) {
            log.error("Evaluation failed. Dto: " + evalDto + " meta: " + metaNode);
            return false;
        }
    }

    private Object getConvertedValue(JsonNode treeValue, Class<Object> type) {

        if (type == null || treeValue == null || treeValue.isNull() || treeValue.isMissingNode()) {
            return null;
        }

        try {
            return objectMapper.treeToValue(treeValue, type);
        } catch (Exception e) {
            log.error("Value convertion failed. Type: " + type + " value: " + treeValue);
        }
        return null;
    }
}
