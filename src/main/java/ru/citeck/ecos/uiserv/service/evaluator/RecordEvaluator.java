package ru.citeck.ecos.uiserv.service.evaluator;

import ru.citeck.ecos.records2.RecordRef;

import java.util.Map;

/**
 * @author Roman Makarskiy
 */
public interface RecordEvaluator<T> {

    boolean evaluate(T config, RecordRef record);

    Map<String, String> getAttributes(T config);

    Class<T> getConfigType();
}
