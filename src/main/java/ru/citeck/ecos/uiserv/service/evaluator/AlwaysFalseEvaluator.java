package ru.citeck.ecos.uiserv.service.evaluator;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;

/**
 * @author Roman Makarskiy
 */
@Component("always-false")
public class AlwaysFalseEvaluator implements RecordEvaluator{
    @Override
    public boolean evaluate(Object config, RecordRef record) {
        return false;
    }
}
