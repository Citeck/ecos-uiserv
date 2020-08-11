package ru.citeck.ecos.uiserv.domain.action.api.records;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
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

    private final ActionService actionService;

    {
        setId("record-actions");
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                        @NotNull MetaField field) {

        RecordActionsQuery query = recordsQuery.getQuery(RecordActionsQuery.class);

        List<RecordRef> targetRefs = query.getRecords()
            .stream()
            .map(rec -> rec.withDefaultAppName("alfresco"))
            .collect(Collectors.toList());

        RecordsActionsDto actionsForRecords = actionService.getActionsForRecords(targetRefs, query.actions);

        int actionsSize = actionsForRecords.getRecordActions().size();
        Long[] recordsActions = new Long[actionsSize];

        List<ActionDto> actions = actionsForRecords.getActions();
        List<String> actionIds = new ArrayList<>();
        actions.forEach(a -> actionIds.add(a.getId()));

        actionsForRecords.getRecordActions().forEach((ref, refActions) -> {
            long flags = 0;
            for (String actionId : refActions) {
                flags |= 1 << actionIds.indexOf(actionId);
            }
            recordsActions[targetRefs.indexOf(ref)] = flags;
        });

        return RecordsQueryResult.of(new ActionsResponse(actions, Arrays.asList(recordsActions)));
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
        private final List<Long> records;
    }
}
