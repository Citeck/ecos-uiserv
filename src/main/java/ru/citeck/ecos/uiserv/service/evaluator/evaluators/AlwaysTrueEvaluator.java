package ru.citeck.ecos.uiserv.service.evaluator.evaluators;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluator;

import java.util.Map;

/**
 * @author Roman Makarskiy
 */
@Component
public class AlwaysTrueEvaluator implements RecordEvaluator<Object, Object> {

    public static final String ID = "true";

    @Override
    public boolean evaluate(Object config, Object meta) {
        return true;
    }

    @Override
    public Map<String, String> getMetaAttributes(Object config) {
        return null;
    }

    @Override
    public Class<Object> getConfigType() {
        return null;
    }

    @Override
    public Class<Object> getMetaType() {
        return null;
    }

    @Override
    public String getId() {
        return "true";
    }
}
