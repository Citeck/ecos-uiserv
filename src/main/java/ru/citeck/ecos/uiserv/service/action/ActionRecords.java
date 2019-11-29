package ru.citeck.ecos.uiserv.service.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.module.type.type.action.ActionDto;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Component
public class ActionRecords extends LocalRecordsDAO
                           implements RecordsQueryWithMetaLocalDAO<ActionRecords.RecordActions> {

    public static final String ID = "action";

    private RecordsService recordsService;

    @Autowired
    public ActionRecords(RecordsService recordsService) {
        setId(ID);
        this.recordsService = recordsService;
    }

    @Override
    public RecordsQueryResult<ActionRecords.RecordActions> getMetaValues(RecordsQuery recordsQuery) {

        ActionsQuery query = recordsQuery.getQuery(ActionsQuery.class);

        List<RecordRef> recordRefs = new ArrayList<>();
        Map<RecordRef, RecordRef> refsMapping = new HashMap<>();

        for (RecordRef ref : query.getRecords()) {
            if (!ref.getAppName().isEmpty()) {
                recordRefs.add(ref);
            } else {
                RecordRef refWithApp = ref.addAppName("alfresco");
                refsMapping.put(refWithApp, ref);
                recordRefs.add(refWithApp);
            }
        }

        RecordsResult<RecordActionsMeta> metaResult = recordsService.getMeta(recordRefs, RecordActionsMeta.class);

        //check _etype, load additional actions from Ecos Type service
        //Use set to get rid of duplicates
        Set<RecordRef> recordsToQueryType = new HashSet<>();
        metaResult.getRecords().forEach(r -> {
            if (StringUtils.isNotBlank(r.type)) {
                recordsToQueryType.add(RecordRef.create("emodel", "type", r.type));
            }
        });
        RecordsResult<TypeActionsMeta> manyTypesActions = recordsService.getMeta(recordsToQueryType, TypeActionsMeta.class);

        Map<String, List<ActionDto>> manyTypesActionsMap = new HashMap<>();
        manyTypesActions.getRecords().forEach(r -> manyTypesActionsMap.put(r.type, r.actions));

        List<RecordActions> resultList = new ArrayList<>();

        int idx = 0;
        for (RecordActionsMeta meta : metaResult.getRecords()) {

            RecordActions actions = new RecordActions();
            RecordRef ref = recordRefs.get(idx++);
            ref = refsMapping.getOrDefault(ref, ref);

            actions.setRecord(ref.toString());

            if (meta.type != null && manyTypesActionsMap.containsKey(meta.type)) {

                actions.setActions(filterActions(
                    manyTypesActionsMap.get(meta.type),
                    meta.getActions()));
            } else if (meta.getActions() != null) {
                actions.setActions(meta.getActions());
            } else {
                actions.setActions(Collections.emptyList());
            }

            resultList.add(actions);
        }

        RecordsQueryResult<RecordActions> queryResult = new RecordsQueryResult<>();
        queryResult.setRecords(resultList);

        return queryResult;
    }

    private List<ActionDto> filterActions(List<ActionDto> typeActions, List<ActionDto> metaActions) {
        List<ActionDto> filteredActions = new ArrayList<>();
        typeActions.forEach(typeAction -> {
            if ("record-actions".equals(typeAction.getType())) {
                JsonNode typeActionConfigKey = typeAction.getConfig().get("key");
                if (typeActionConfigKey != null && !typeActionConfigKey.isNull()) {
                    Pattern configKeyPattern = Pattern.compile(
                        typeActionConfigKey.asText()
                            .replace(".", "\\.")
                            .replace("*", "[^.]+")
                            .replace("#", ".*"));

                    metaActions.forEach(recordAction -> {
                        if (configKeyPattern.matcher(recordAction.getKey()).matches()) {
                            filteredActions.add(recordAction);
                        }
                    });
                } else {
                    filteredActions.addAll(metaActions);
                }
            } else {
                filteredActions.add(typeAction);
            }
        });
        return filteredActions;
    }

    @Data
    public static class RecordActionsMeta {
        @MetaAtt("_etype")
        private String type;
        @MetaAtt("_actions[]")
        private List<ActionDto> actions;
    }

    @Data
    public static class TypeActionsMeta {
        @MetaAtt(".id")
        private String type;
        @MetaAtt("_actions[]")
        private List<ActionDto> actions;
    }

    @Data
    public static class ActionsQuery {
        private List<RecordRef> records;
        private JsonNode predicate;
    }

    @Data
    public static class RecordActions {

        private String id = UUID.randomUUID().toString();

        private String record;
        private List<ActionDto> actions;
    }
}
