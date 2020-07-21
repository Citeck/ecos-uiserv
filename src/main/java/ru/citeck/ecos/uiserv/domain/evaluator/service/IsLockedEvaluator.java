package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.Data;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;

@Component
public class IsLockedEvaluator implements RecordEvaluator<Class<IsLockedEvaluator.Meta>, IsLockedEvaluator.Meta,
    IsLockedEvaluator.Config> {

    private static final String TYPE = "is-locked";

    @Override
    public Class<IsLockedEvaluator.Meta> getMetaToRequest(Config config) {
        return IsLockedEvaluator.Meta.class;
    }

    @Override
    public boolean evaluate(Meta meta, Config config) {
        return Boolean.TRUE.equals(meta.isLocked);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Data
    public static class Meta {
        private Boolean isLocked;
    }

    @Data
    public static class Config {
    }
}
