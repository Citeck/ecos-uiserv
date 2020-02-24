package ru.citeck.ecos.uiserv.service.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.module.ModuleRef;
import ru.citeck.ecos.apps.app.module.type.ui.action.ActionModule;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.commons.utils.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Component
public class ActionRecords extends LocalRecordsDAO
                           implements LocalRecordsQueryWithMetaDAO<ActionRecords.RecordActions> {

    private static final String RECORD_ACTIONS_TYPE = "record-actions";

    public static final String ID = "action";

    private RecordsService recordsService;
    private ActionService actionService;

    @Autowired
    public ActionRecords(RecordsService recordsService, ActionService actionService) {
        setId(ID);
        this.recordsService = recordsService;
        this.actionService = actionService;
    }

    @Override
    public RecordsQueryResult<RecordActions> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {

        ActionsQuery query = recordsQuery.getQuery(ActionsQuery.class);

        List<RecordInfo> recordsInfo = query.getRecords()
            .stream()
            .map(RecordInfo::new)
            .collect(Collectors.toList());

        if (query.actions != null) {
            recordsInfo.forEach(info -> {
                if (info.actionIds == null) {
                    info.actionIds = new ArrayList<>();
                }
                info.actionIds.addAll(query.actions);
            });
        }

        fillActionsFromType(recordsInfo.stream()
            .filter(info -> info.actionIds == null)
            .collect(Collectors.toList()));

        expandActionIds(recordsInfo);
        fillRecordActions(recordsInfo);

        List<RecordActions> resultList = new ArrayList<>();

        for (RecordInfo info : recordsInfo) {

            List<ActionModule> actions = expandResultActions(info.getResultActions(), info.getRecordActions());
            if (actions == null) {
                actions = Collections.emptyList();
            }
            RecordActions recordActions = new RecordActions();
            recordActions.setRecord(info.getOriginalRecordRef().toString());
            recordActions.setActions(actions);
            resultList.add(recordActions);
        }

        RecordsQueryResult<RecordActions> queryResult = new RecordsQueryResult<>();
        queryResult.setRecords(resultList);

        return queryResult;
    }

    private void fillRecordsType(List<RecordInfo> recordsInfo) {

        List<RecordRef> recordRefs = recordsInfo.stream().map(RecordInfo::getRecordRef).collect(Collectors.toList());

        RecordsResult<RecordTypeMeta> recordsTypesResult = recordsService.getMeta(recordRefs, RecordTypeMeta.class);
        List<RecordTypeMeta> recordTypes = recordsTypesResult.getRecords();

        for (int i = 0; i < recordTypes.size(); i++) {
            String type = recordTypes.get(i).type;
            if (i < recordsInfo.size() && StringUtils.isNotBlank(type)) {
                recordsInfo.get(i).setType(RecordRef.valueOf(type));
            }
        }
    }

    private void expandActionIds(List<RecordInfo> recordsInfo) {

        Map<Set<ModuleRef>, List<RecordInfo>> recordsByActionsList = new HashMap<>();

        recordsInfo.forEach(info -> {
            if (info.getActionIds() != null && !info.getActionIds().isEmpty()) {
                Set<ModuleRef> key = new HashSet<>(info.getActionIds());
                recordsByActionsList.computeIfAbsent(key, ids -> new ArrayList<>()).add(info);
            }
        });

        recordsByActionsList.forEach((actions, records) -> {

            List<RecordRef> refs = records.stream()
                .map(RecordInfo::getRecordRef)
                .collect(Collectors.toList());

            Map<RecordRef, List<ActionModule>> actionsRes = actionService.getActions(refs, new ArrayList<>(actions));

            records.forEach(info -> {
                List<ActionModule> recordActions = new ArrayList<>(actionsRes.get(info.getRecordRef()));
                recordActions.sort((a0, a1) -> {
                    List<ModuleRef> recordActionIds = info.getActionIds();
                    int idx0 = recordActionIds.indexOf(ModuleRef.create("ui/action", a0.getId()));
                    int idx1 = recordActionIds.indexOf(ModuleRef.create("ui/action", a1.getId()));
                    return Integer.compare(idx0, idx1);
                });
                info.setResultActions(recordActions);
            });
        });
    }

    private void fillActionsFromType(List<RecordInfo> recordsInfo) {

        fillRecordsType(recordsInfo);

        Map<RecordRef, List<RecordInfo>> recordsByType = new HashMap<>();

        for (RecordInfo info : recordsInfo) {
            RecordRef type = info.getType();
            if (type != null) {
                recordsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(info);
            }
        }

        List<RecordRef> typesRefs = new ArrayList<>(recordsByType.keySet());
        RecordsResult<ActionsRefMeta> typesActionsResult = recordsService.getMeta(typesRefs, ActionsRefMeta.class);

        List<ActionsRefMeta> typesActions = typesActionsResult.getRecords();

        for (int i = 0; i < typesActions.size(); i++) {
            ActionsRefMeta typeActions = typesActions.get(i);
            RecordRef typeRef = typesRefs.get(i);
            recordsByType.get(typeRef).forEach(info -> info.setActionIds(typeActions.getActions()));
        }
    }

    private void fillRecordActions(List<RecordInfo> recordsInfo) {

        List<RecordRef> recordsToQueryActions = recordsInfo.stream().filter(info ->
                   info.getActionIds() == null
                || info.getResultActions() == null
                || info.getResultActions()
                       .stream()
                       .anyMatch(a -> RECORD_ACTIONS_TYPE.equals(a.getType()))
        ).map(RecordInfo::getRecordRef).collect(Collectors.toList());

        RecordsResult<ActionsDtoMeta> actionsResult = recordsService.getMeta(recordsToQueryActions,
                                                                             ActionsDtoMeta.class);
        List<ActionsDtoMeta> actions = actionsResult.getRecords();

        for (int i = 0; i < actions.size(); i++) {
            if (i < recordsToQueryActions.size()) {
                RecordRef recordRef = recordsToQueryActions.get(i);
                for (RecordInfo info : recordsInfo) {
                    if (Objects.equals(info.getRecordRef(), recordRef)) {
                        info.setRecordActions(actions.get(i).actions);
                    }
                }
            }
        }
    }

    private List<ActionModule> expandResultActions(List<ActionModule> resultActions, List<ActionModule> recordActions) {
        if (resultActions == null) {
            return recordActions;
        }
        if (resultActions.isEmpty()) {
            return Collections.emptyList();
        }
        List<ActionModule> filteredActions = new ArrayList<>();
        resultActions.forEach(action -> {
            if ("record-actions".equals(action.getType())) {
                ObjectData config = action.getConfig();
                DataValue typeActionConfigKey = config != null ? config.get("key") : null;
                if (typeActionConfigKey != null && !typeActionConfigKey.isNull()) {
                    Pattern configKeyPattern = Pattern.compile(
                        typeActionConfigKey.asText()
                            .replace(".", "\\.")
                            .replace("*", "[^.]+")
                            .replace("#", ".*"));

                    recordActions.forEach(recordAction -> {

                        if (recordAction.getKey() != null &&
                                configKeyPattern.matcher(recordAction.getKey()).matches()) {

                            filteredActions.add(recordAction);
                        }
                    });
                } else {
                    filteredActions.addAll(recordActions);
                }
            } else {
                filteredActions.add(action);
            }
        });
        return filteredActions;
    }

    @Data
    public static class RecordTypeMeta {
        @MetaAtt("_etype?id")
        private String type;
    }

    @Data
    public static class ActionsDtoMeta {
        @MetaAtt("_actions[]")
        private List<ActionModule> actions;
    }

    @Data
    public static class ActionsRefMeta {
        @MetaAtt("_actions[]?str")
        private List<ModuleRef> actions;
    }

    @Data
    public static class ActionsQuery {
        private List<JsonNode> records;
        private List<ModuleRef> actions;
    }

    @Data
    public static class RecordActions {

        private String id = UUID.randomUUID().toString();

        private String record;
        private List<ActionModule> actions;
    }

    @Data
    private static class RecordInfo {

        private final RecordRef recordRef;
        private final RecordRef originalRecordRef;

        private RecordRef type;

        private List<ActionModule> recordActions = Collections.emptyList();
        private List<ActionModule> resultActions = Collections.emptyList();

        private List<ModuleRef> actionIds;

        public RecordInfo(JsonNode record) {

            if (record instanceof TextNode) {
                originalRecordRef = RecordRef.valueOf(record.asText());
            } else if (record instanceof ObjectNode) {
                originalRecordRef = RecordRef.valueOf(record.get("record").asText());
                if (record.has("actions")) {
                    actionIds = new ArrayList<>();
                    record.get("actions").forEach(action -> {
                        actionIds.add(ModuleRef.valueOf(action.asText()));
                    });
                }
            } else {
                throw new IllegalArgumentException("Incorrect record info: " + record);
            }

            if (!originalRecordRef.getAppName().isEmpty()) {
                recordRef = originalRecordRef;
            } else {
                recordRef = originalRecordRef.addAppName("alfresco");
            }
        }
    }
}
