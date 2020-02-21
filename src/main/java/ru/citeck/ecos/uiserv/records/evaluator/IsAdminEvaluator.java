package ru.citeck.ecos.uiserv.records.evaluator;

import lombok.Data;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.Collections;
import java.util.Map;

public class IsAdminEvaluator implements RecordEvaluator<Map<String, String>, IsAdminEvaluator.Meta,
    IsAdminEvaluator.Config> {

    private static final String TYPE = "is-admin";

    @Override
    public Map<String, String> getMetaToRequest(Config config) {
        return Collections.singletonMap("admin", "$user.isAdmin?bool");
    }

    @Override
    public boolean evaluate(Meta meta, Config config) {
        return meta.isAdmin;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Data
    public static class Config {
    }

    @Data
    public static class Meta {
        @MetaAtt("$user.isAdmin?bool")
        private boolean isAdmin;
    }
}
