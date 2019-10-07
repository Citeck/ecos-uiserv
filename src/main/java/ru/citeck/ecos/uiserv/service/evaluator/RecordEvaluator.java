package ru.citeck.ecos.uiserv.service.evaluator;

import ru.citeck.ecos.records2.RecordRef;

/**
 * @author Roman Makarskiy
 */
public interface RecordEvaluator {

    boolean evaluate(Object config, RecordRef record);

}
