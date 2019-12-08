package ru.citeck.ecos.uiserv.service.action;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.module.ModuleRef;
import ru.citeck.ecos.apps.app.module.type.ui.action.ActionDto;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.records2.utils.StringUtils;

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

    @Autowired
    public ActionRecords(RecordsService recordsService) {
        setId(ID);
        this.recordsService = recordsService;
    }

    @Override
    public RecordsQueryResult<RecordActions> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {

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

        List<RecordInfo> recordsInfo = recordRefs.stream().map(RecordInfo::new).collect(Collectors.toList());
        fillRecordsType(recordsInfo);
        fillTypeActions(recordsInfo);
        fillRecordActions(recordsInfo);

        List<RecordActions> resultList = new ArrayList<>();

        for (RecordInfo info : recordsInfo) {
            List<ActionDto> actions = filterActions(info.getTypeActionsRefs(), info.getRecordActions());
            if (actions == null) {
                actions = Collections.emptyList();
            }
            RecordActions recordActions = new RecordActions();
            recordActions.setRecord(refsMapping.getOrDefault(info.getRecordRef(), info.getRecordRef()).toString());
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

    private void fillTypeActions(List<RecordInfo> recordsInfo) {

        List<RecordRef> typesToQueryActions = recordsInfo
            .stream()
            .filter(rec -> rec.getType() != null)
            .map(RecordInfo::getType)
            .distinct()
            .collect(Collectors.toList());

        RecordsResult<ActionsRefMeta> typesActionsResult = recordsService.getMeta(typesToQueryActions, ActionsRefMeta.class);
        List<ActionsRefMeta> typesActions = typesActionsResult.getRecords();

        for (int i = 0; i < typesActions.size(); i++) {
            if (i < typesToQueryActions.size()) {
                RecordRef typeRef = typesToQueryActions.get(i);



                for (RecordInfo info : recordsInfo) {
                    if (Objects.equals(typeRef, info.getType())) {
                        info.setTypeActionsRefs(typesActions.get(i).getActions());
                    }
                }
            }
        }
    }

    private void fillRecordActions(List<RecordInfo> recordsInfo) {

        List<RecordRef> recordsToQueryActions = recordsInfo.stream().filter(info ->
            info.getTypeActionsRefs() == null || info.getTypeActionsRefs()
                       .stream()
                       .anyMatch(a -> RECORD_ACTIONS_TYPE.equals(a.getType()))
        ).map(RecordInfo::getRecordRef).collect(Collectors.toList());

        RecordsResult<ActionsDtoMeta> actionsResult = recordsService.getMeta(recordsToQueryActions, ActionsDtoMeta.class);
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

    private List<ActionDto> filterActions(List<ActionDto> typeActions, List<ActionDto> recordActions) {
        if (typeActions == null) {
            return recordActions;
        }
        if (typeActions.isEmpty()) {
            return Collections.emptyList();
        }
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
                filteredActions.add(typeAction);
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
        private List<ActionDto> actions;
    }

    @Data
    public static class ActionsRefMeta {
        @MetaAtt("_actions[]?str")
        private List<ModuleRef> actions;
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

    @Data
    private static class RecordInfo {

        private RecordRef recordRef;
        private RecordRef type;
        private List<ActionDto> recordActions;
        private List<ModuleRef> typeActionsRefs;

        public RecordInfo(RecordRef recordRef) {
            this.recordRef = recordRef;
        }
    }
}
