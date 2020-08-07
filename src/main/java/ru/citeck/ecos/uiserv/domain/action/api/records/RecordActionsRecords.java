package ru.citeck.ecos.uiserv.domain.action.api.records;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto;
import ru.citeck.ecos.uiserv.domain.action.dto.RecordsActionsDto;
import ru.citeck.ecos.uiserv.domain.action.service.ActionService;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecordActionsRecords extends LocalRecordsDao implements LocalRecordsQueryWithMetaDao<Object> {

    private static final String RECORD_ACTIONS_TYPE = "record-actions";

    private final ActionService actionService;

    {
        setId("record-actions");
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                        @NotNull MetaField field) {

        RecordActionsQuery query = recordsQuery.getQuery(RecordActionsQuery.class);

        RecordsActionsDto actionsForRecords = actionService.getActionsForRecords(query.records, query.actions);

        List<RecordRef> recordsWithAttActions = new ArrayList<>();
        if (query.getActions().contains(RecordRef.valueOf("uiserv/action@record-actions"))) {
            actionsForRecords.getRecordActions().forEach((ref, actionIds) -> {
                for (int i = 0; i < query.actions.size(); i++) {
                    ActionDto actionDto = actionsForRecords.getActions().get(i);
                    if (actionDto.getId().equals(RECORD_ACTIONS_TYPE)) {
                        recordsWithAttActions.add(ref);
                        break;
                    }
                }
            });
        }

        Map<RecordRef, List<AttributeActionMeta>> attActionsByRecord = new HashMap<>();
        if (!recordsWithAttActions.isEmpty()) {

            List<RecordRef> recordsToQuery = recordsWithAttActions.stream()
                .map(r -> r.withDefaultAppName("alfresco"))
                .collect(Collectors.toList());

            List<RecordAttributeActionMeta> attActions = recordsService.getMeta(recordsToQuery,
                                                                          RecordAttributeActionMeta.class).getRecords();

            for (int i = 0; i < recordsWithAttActions.size(); i++) {
                List<AttributeActionMeta> actions = attActions.get(i).getActions();
                if (actions == null) {
                    actions = Collections.emptyList();
                }
                attActionsByRecord.put(recordsWithAttActions.get(i), actions);
            }
        }

        List<RecordActions> recordActions = new ArrayList<>();
        actionsForRecords.getRecordActions().forEach((ref, actionIds) -> {
            List<Integer> hasActionList = new ArrayList<>();
            List<ActionDto> attributeActions = Collections.emptyList();
            for (int i = 0; i < query.actions.size(); i++) {
                ActionDto actionDto = actionsForRecords.getActions().get(i);
                if (RECORD_ACTIONS_TYPE.equals(actionDto.getType())) {
                    hasActionList.add(0);
                    attributeActions = DataValue.create(
                        attActionsByRecord.getOrDefault(ref, Collections.emptyList())).asList(ActionDto.class);
                } else {
                    hasActionList.add(actionIds.contains(actionDto.getId()) ? 1 : 0);
                }
            }
            recordActions.add(new RecordActions(ref.toString(), hasActionList, attributeActions));
        });

        List<ActionDto> actions = actionsForRecords.getActions()
            .stream()
            .filter(a -> !a.getId().equals(RECORD_ACTIONS_TYPE))
            .collect(Collectors.toList());

        return RecordsQueryResult.of(new ActionsResponse(actions, recordActions));
    }

    @Data
    public static class RecordAttributeActionMeta {
        @MetaAtt("_actions[]")
        private List<AttributeActionMeta> actions;
    }

    @Data
    public static class AttributeActionMeta {
        private String id;
        private String name;
        private String icon;
        private String type;
        private ObjectData config = ObjectData.create();
    }

    @Data
    public static class RecordActionsQuery {
        private List<RecordRef> records;
        private List<RecordRef> actions;
    }

    @Data
    @RequiredArgsConstructor
    public static class ActionsResponse {

        private final String id = UUID.randomUUID().toString();

        private final List<ActionDto> actions;
        private final List<RecordActions> records;
    }

    @Data
    @RequiredArgsConstructor
    public static class RecordActions {
        private final String record;
        private final List<Integer> actions;
        private final List<ActionDto> attributeActions;
    }
}
