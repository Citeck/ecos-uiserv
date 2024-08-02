package ru.citeck.ecos.uiserv.domain.action.api.records;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto;
import ru.citeck.ecos.uiserv.domain.action.dto.RecordsActionsDto;
import ru.citeck.ecos.uiserv.domain.action.service.ActionService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecordActionsRecords extends AbstractRecordsDao implements RecordsQueryDao {

    private final ActionService actionService;

    @Nullable
    @Override
    public Object queryRecords(@NotNull RecordsQuery recordsQuery) throws Exception {

        RecordActionsQuery query = recordsQuery.getQuery(RecordActionsQuery.class);

        List<EntityRef> targetRefs = query.getRecords()
            .stream()
            .map(rec -> rec.withDefaultAppName("alfresco"))
            .collect(Collectors.toList());

        List<EntityRef> queryActions = query.actions;
        if (queryActions == null) {
            queryActions = Collections.emptyList();
        }
        RecordsActionsDto actionsForRecords = actionService.getActionsForRecords(targetRefs, queryActions);

        List<ActionDto> actions = actionsForRecords.getActions();
        List<String> actionIds = new ArrayList<>();
        actions.forEach(a -> actionIds.add(a.getId()));

        Long[] recordsActionsMask = new Long[targetRefs.size()];

        Map<EntityRef, Set<String>> recordActions = actionsForRecords.getRecordActions();
        for (int idx = 0; idx < targetRefs.size(); idx++) {
            EntityRef ref = targetRefs.get(idx);
            Set<String> refActions = recordActions.get(ref);
            long flags = 0;
            for (String actionId : refActions) {
                flags |= 1L << actionIds.indexOf(actionId);
            }
            recordsActionsMask[idx] = flags;
        }

        return new ActionsResponse(actions, Arrays.asList(recordsActionsMask));
    }

    @NotNull
    @Override
    public String getId() {
        return "record-actions";
    }

    @Data
    public static class RecordActionsQuery {
        private List<EntityRef> records;
        private List<EntityRef> actions;
    }

    @Data
    @RequiredArgsConstructor
    public static class ActionsResponse {

        private final String id = UUID.randomUUID().toString();

        private final List<ActionDto> actions;
        private final List<Long> records;
    }
}
