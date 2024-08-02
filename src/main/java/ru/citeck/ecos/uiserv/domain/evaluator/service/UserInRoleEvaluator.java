package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class UserInRoleEvaluator implements RecordEvaluator<Map<String, String>, Map<String, Boolean>,
    UserInRoleEvaluator.Config> {

    private static final String TYPE = "user-in-role";
    private static final String REQUEST_META = ".att(n:\"case-roles\"){att(n:\"%s\"){has(n:\"$CURRENT\")}}";

    @Override
    public Map<String, String> getMetaToRequest(Config config) {

        Map<String, String> resultMap;

        if (CollectionUtils.isNotEmpty(config.getAnyRole())) {
            resultMap = new HashMap<>();
            for (String role : config.getAnyRole()) {
                String metaValue = String.format(REQUEST_META, role);
                resultMap.put(role, metaValue);
            }
            return resultMap;
        }

        if (config.getRole() == null) {
            return Collections.emptyMap();
        }

        String role = config.getRole();
        String metaValue = String.format(REQUEST_META, role);
        return Collections.singletonMap(role, metaValue);
    }

    @Override
    public boolean evaluate(Map<String, Boolean> meta, Config config) {
        for (Map.Entry<String, Boolean> entry : meta.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Data
    public static class Config {
        private String role;
        private Set<String> anyRole;
    }
}
