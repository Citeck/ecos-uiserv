package ru.citeck.ecos.uiserv.service.action;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.*;

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

    private List<ActionDto> getActions() {

        List<ActionDto> result = new ArrayList<>();

        ActionDto action = new ActionDto();
        action.setName("grid.inline-tools.edit");
        action.setIcon("icon-edit");
        action.setType("edit");
        action.setKey("dao.edit");

        result.add(action);

        action = new ActionDto();
        action.setName("grid.inline-tools.show");
        action.setIcon("icon-on");
        action.setType("view");
        action.setKey("dto.view");

        result.add(action);

        action = new ActionDto();
        action.setName("grid.inline-tools.download");
        action.setIcon("icon-download");
        action.setType("download");
        action.setKey("dto.download");

        result.add(action);

        action = new ActionDto();
        action.setName("grid.inline-tools.delete");
        action.setIcon("icon-delete");
        action.setType("delete");
        action.setKey("dto.delete");

        result.add(action);

        return result;
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

        List<RecordActions> resultList = new ArrayList<>();

        int idx = 0;
        for (RecordActionsMeta meta : metaResult.getRecords()) {

            RecordActions actions = new RecordActions();
            RecordRef ref = recordRefs.get(idx++);
            ref = refsMapping.getOrDefault(ref, ref);

            actions.setRecord(ref.toString());

            if (meta.getActions() != null) {
                actions.setActions(meta.getActions());
            } else {
                actions.setActions(Collections.emptyList());
            }

            actions.setActions(getActions());//todo: temp

            resultList.add(actions);
        }

        RecordsQueryResult<RecordActions> result = new RecordsQueryResult<>();
        result.setRecords(resultList);

        return result;
    }

    @Data
    public static class RecordActionsMeta {
        @MetaAtt("_etype")
        private String type;
        @MetaAtt("_actions")
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
