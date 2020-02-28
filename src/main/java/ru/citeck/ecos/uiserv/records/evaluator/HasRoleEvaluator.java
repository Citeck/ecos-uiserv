package ru.citeck.ecos.uiserv.records.evaluator;

import lombok.Data;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;

import java.util.Collections;
import java.util.Map;

@Component
public class HasRoleEvaluator implements RecordEvaluator<Map<String, String>, HasRoleEvaluator.Meta,
    HasRoleEvaluator.Config> {

    private static final String TYPE = "has-role";

    @Override
    public Map<String, String> getMetaToRequest(Config config) {
        String role = config.getRole();
        String metaValue = String.format(".att(n:\"case-roles\"){att(n:\"%s\"){has(n:\"$CURRENT\")}}", role);
        return Collections.singletonMap("role", metaValue);
    }

    @Override
    public boolean evaluate(Meta meta, Config config) {
        return Boolean.TRUE.equals(meta.role);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Data
    public static class Meta {
        private Boolean role;
    }

    @Data
    public static class Config {
        private String role;
    }
}
