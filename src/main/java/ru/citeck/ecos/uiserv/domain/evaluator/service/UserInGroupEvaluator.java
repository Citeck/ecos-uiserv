package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.Data;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluator;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class UserInGroupEvaluator implements RecordEvaluator<Map<String, String>,
    Map<String, Boolean>,
    UserInGroupEvaluator.Config> {

    private static final String TYPE = "user-in-group";
    private static final String REQUEST_META = ".att(n:\"$user\"){att(n:\"authorities\"){has(n:\"%s\")}}";

    @Override
    public boolean evaluate(Map<String, Boolean> meta, Config config) {
        return meta.values()
            .stream()
            .anyMatch(Boolean.TRUE::equals);
    }

    @Override
    public Map<String, String> getMetaToRequest(UserInGroupEvaluator.Config config) {

        List<String> groupNames = config.getGroupName();

        if (groupNames == null) {
            throw new IllegalArgumentException("You need to specify authorities for evaluating. Config:" + config);
        }

        return groupNames
            .stream()
            .collect(Collectors.toMap(
                Function.identity(),
                this::groupNameToRequestMeta
            ));
    }

    private String groupNameToRequestMeta(String groupName) {
        return String.format(REQUEST_META, groupName);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Data
    public static class Config {
        private List<String> groupName;
    }
}
