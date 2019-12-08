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
public class RecordHasPermissionEvaluator implements RecordEvaluator<RecordHasPermissionEvaluator.Config,
                                                                     RecordHasPermissionEvaluator.Meta> {

    private static final String PERMISSION_ATT_PATTERN = ".att(n:\"permissions\"){has(n:\"%s\")}";
    private static final String HAS_PERM_PROP = "hasPermission";

    @Override
    public boolean evaluate(Config config, Meta meta) {
        return meta.hasPermission == null || Boolean.TRUE.equals(meta.hasPermission);
    }

    @Override
    public Map<String, String> getMetaAttributes(Config config) {

        if (StringUtils.isBlank(config.permission)) {
            throw new IllegalArgumentException("You need to specify a permission for evaluating. Config:"
                + config.toString());
        }

        return Collections.singletonMap(HAS_PERM_PROP, String.format(PERMISSION_ATT_PATTERN, config.permission));
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public Class<Meta> getMetaType() {
        return Meta.class;
    }

    @Override
    public String getId() {
        return "has-permission";
    }

    @Data
    public static class Config {
        private String permission;
    }

    @Data
    public static class Meta {
        private Boolean hasPermission;
    }
}
