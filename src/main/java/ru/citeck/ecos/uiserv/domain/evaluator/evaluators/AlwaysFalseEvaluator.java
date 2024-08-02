package ru.citeck.ecos.uiserv.domain.evaluator.evaluators;

import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluator;

public class AlwaysFalseEvaluator implements RecordEvaluator<Object, Object, Object> {

    public static final String TYPE = "false";

    @Override
    public boolean evaluate(Object config, Object meta) {
        return false;
    }

    @Override
    public Object getMetaToRequest(Object config) {
        return null;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
