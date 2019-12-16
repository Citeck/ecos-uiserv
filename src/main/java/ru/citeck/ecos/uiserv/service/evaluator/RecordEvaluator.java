package ru.citeck.ecos.uiserv.service.evaluator;

import java.util.Map;

/**
 * @author Roman Makarskiy
 */
public interface RecordEvaluator<C, M> {

    boolean evaluate(C config, M meta);

    Map<String, String> getMetaAttributes(C config);

    Class<C> getConfigType();

    Class<M> getMetaType();

    String getId();
}
