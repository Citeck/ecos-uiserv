package ru.citeck.ecos.uiserv.domain.evaluator.evaluators;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.uiserv.domain.evaluator.details.EvalDetails;
import ru.citeck.ecos.uiserv.domain.evaluator.details.EvalDetailsImpl;
import ru.citeck.ecos.uiserv.domain.evaluator.details.EvalResultCause;
import ru.citeck.ecos.uiserv.domain.evaluator.details.RecordEvaluatorWithDetails;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class GroupEvaluator
    implements RecordEvaluatorWithDetails<Map<String, String>, RecordAtts, GroupEvaluator.Config> {

    public static final String TYPE = "group";

    private final RecordEvaluatorService recordEvaluatorService;

    @Override
    public EvalDetails evalWithDetails(RecordAtts meta, Config config) {

        Stream<RecordEvaluatorDto> evaluators = config.getEvaluators().stream();

        List<EvalResultCause> causes = new ArrayList<>();

        Predicate<RecordEvaluatorDto> predicate = evaluator -> {
            EvalDetails evalDetails = recordEvaluatorService.evalDetailsWithMeta(evaluator, new RecordAtts(meta));
            boolean result = evalDetails != null && evalDetails.getResult();
            if (!result && evalDetails != null) {
                causes.addAll(evalDetails.getCauses());
            }
            return result;
        };

        boolean result = false;

        if (JoinType.AND.equals(config.joinBy)) {
            result = evaluators.allMatch(predicate);
        } else if (JoinType.OR.equals(config.joinBy)) {
            result = evaluators.anyMatch(predicate);
        } else {
            log.warn("Unknown join type: {}", config.joinBy);
        }

        return new EvalDetailsImpl(result, causes);
    }

    @Override
    public Map<String, String> getMetaToRequest(Config config) {

        Set<String> atts = new HashSet<>();

        config.getEvaluators()
            .stream()
            .map(recordEvaluatorService::getRequiredMetaAttributes)
            .forEach(a -> atts.addAll(a.values()));

        Map<String, String> result = new HashMap<>();
        for (String att : atts) {
            result.put(att, att);
        }

        return result;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Data
    public static class Config {

        private JoinType joinBy = JoinType.AND;
        private List<RecordEvaluatorDto> evaluators = Collections.emptyList();
    }

    public enum JoinType { AND, OR }
}
