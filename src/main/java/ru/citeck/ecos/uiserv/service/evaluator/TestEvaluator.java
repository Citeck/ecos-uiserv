package ru.citeck.ecos.uiserv.service.evaluator;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;

/**
 * @author Roman Makarskiy
 */
@Component("test-evaluator")
public class TestEvaluator implements RecordEvaluator {
    @Override
    public boolean evaluate(Object config, RecordRef record) {
        return record.getId().contains("test");
    }
}
