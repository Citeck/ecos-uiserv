package ru.citeck.ecos.uiserv.service.evaluator.evaluators;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluator;

import java.util.Collections;
import java.util.Map;

/**
 * @author Roman Makarskiy
 */
@Component("always-false")
public class AlwaysFalseEvaluator implements RecordEvaluator {
    @Override
    public boolean evaluate(Object config, RecordRef record) {
        return false;
    }

    @Override
    public Map<String, String> getAttributes(Object config) {
        return Collections.emptyMap();
    }

    @Override
    public Class getConfigType() {
        return Object.class;
    }
}
