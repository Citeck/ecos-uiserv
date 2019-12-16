package ru.citeck.ecos.uiserv.service.evaluator.evaluators;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluator;

import java.util.Collections;
import java.util.Map;

/**
 * @author Roman Makarskiy
 */
@Component
public class RecordHasAttributeEvaluator implements RecordEvaluator<RecordHasAttributeEvaluator.Config,
                                                                    RecordHasAttributeEvaluator.Meta> {

    private static final String HAS_ATTRIBUTE_PATTERN = ".has(n:\"%s\")";
    private static final String HAS_ATT_META_FIELD = "hasAtt";

    @Override
    public boolean evaluate(Config config, Meta meta) {
        return Boolean.TRUE.equals(meta.hasAtt);
    }

    @Override
    public Map<String, String> getMetaAttributes(Config config) {

        if (StringUtils.isBlank(config.attribute)) {
            throw new IllegalArgumentException("You need to specify a attribute, for evaluating. Config:"
                + config.toString());
        }

        return Collections.singletonMap(HAS_ATT_META_FIELD, String.format(HAS_ATTRIBUTE_PATTERN, config.attribute));
    }

    @Override
    public Class<Meta> getMetaType() {
        return Meta.class;
    }

    @Override
    public String getId() {
        return "has-attribute";
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Data
    public static class Config {
        private String attribute;
    }

    @Data
    public static class Meta {
        private Boolean hasAtt;
    }
}
