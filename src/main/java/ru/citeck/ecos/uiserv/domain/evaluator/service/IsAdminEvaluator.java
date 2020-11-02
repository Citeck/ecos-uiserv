package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.Data;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;

@Component
public class IsAdminEvaluator implements RecordEvaluator<Class<IsAdminEvaluator.Meta>, IsAdminEvaluator.Meta,
    IsAdminEvaluator.Config> {

    private static final String TYPE = "is-admin";

    @Override
    public Class<Meta> getMetaToRequest(Config config) {
        return Meta.class;
    }

    @Override
    public boolean evaluate(Meta meta, Config config) {
        return Boolean.TRUE.equals(meta.isAdmin);
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
        @AttName("$user.isAdmin?bool")
        private Boolean isAdmin;
    }
}
