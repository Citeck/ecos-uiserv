package ru.citeck.ecos.uiserv.domain.action.api.records;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import kotlin.Unit;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.events2.type.RecordEventsService;
import ru.citeck.ecos.model.lib.type.repo.TypesRepo;
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.uiserv.domain.action.service.ActionService;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto;
import ru.citeck.ecos.uiserv.domain.utils.LegacyRecordsUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Component
public class ActionRecords extends LocalRecordsDao
                           implements LocalRecordsQueryWithMetaDao<Object>,
                                      LocalRecordsMetaDao<Object>,
                                      MutableRecordsLocalDao<ActionRecords.ActionRecord> {

    private static final String RECORD_ACTIONS_TYPE = "record-actions";

    public static final String ID = "action";

    private final RecordsService recordsService;
    private final ActionService actionService;
    private RecordEventsService recordEventsService;
    private final TypesRepo typesRepo;

    @Autowired
    public ActionRecords(RecordsService recordsService, ActionService actionService, TypesRepo typesRepo) {
        setId(ID);
        this.recordsService = recordsService;
        this.actionService = actionService;
        this.typesRepo = typesRepo;
    }

    @PostConstruct
    public void init() {
        actionService.onActionChanged((before, after) -> {
            if (recordEventsService != null) {
                recordEventsService.emitRecChanged(before, after, getId(), ActionRecord::new);
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        RecordsDelResult result = new RecordsDelResult();
        for (RecordRef record : deletion.getRecords()) {
            actionService.deleteAction(record.getId());
            result.addRecord(new RecordMeta(record));
        }
        return result;
    }

    @Override
    public List<ActionRecord> getValuesToMutate(List<RecordRef> records) {
        List<Object> recordAtts = getLocalRecordsMeta(records, null);
        List<ActionRecord> toMutate = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            Object atts = recordAtts.get(i);
            if (!(atts instanceof ActionRecord)) {
                throw new RuntimeException("Action doesn't found: '" + records.get(i) + "'");
            }
            toMutate.add((ActionRecord) atts);
        }
        return toMutate;
    }

    @Override
    public RecordsMutResult save(List<ActionRecord> values) {
        RecordsMutResult result = new RecordsMutResult();
        values.forEach(value -> {
            actionService.updateAction(value);
            result.addRecord(new RecordMeta(RecordRef.valueOf(value.getId())));
        });
        return result;
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream()
            .map(r -> {
                if (r.getId().isEmpty()) {
                    return new ActionRecord();
                }
                return actionService.getAction(r.getId());
            })
            .map(dto -> {
                if (dto == null) {
                    return EmptyAttValue.INSTANCE;
                } else if (dto instanceof ActionRecord) {
                    return dto;
                } else {
                    return new ActionRecord(dto);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {

        ActionsQuery parsedQuery = null;
        if (StringUtils.isBlank(recordsQuery.getLanguage())) {
            parsedQuery = recordsQuery.getQuery(ActionsQuery.class);
        }

        if (parsedQuery == null) {

            RecordsQueryResult<Object> result = new RecordsQueryResult<>();

            int max = recordsQuery.getMaxItems();
            int skip = recordsQuery.getSkipCount();

            List<ActionDto> actions;

            if (PredicateService.LANGUAGE_PREDICATE.equals(recordsQuery.getLanguage())) {

                Predicate predicate = recordsQuery.getQuery(Predicate.class);

                actions = actionService.getActions(
                    predicate,
                    max,
                    skip,
                    LegacyRecordsUtils.mapLegacySortBy(recordsQuery.getSortBy())
                );
                result.setTotalCount(actionService.getCount(predicate));

            } else {

                actions = actionService.getActions(max, skip);
                result.setTotalCount(actionService.getCount());
            }

            result.setRecords(actions.stream()
                .map(ActionRecord::new)
                .collect(Collectors.toList()));

            return result;
        }

        ActionsQuery query = parsedQuery;

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

            List<ActionDto> actions = expandResultActions(info.getResultActions(), info.getRecordActions());
            if (actions == null) {
                actions = Collections.emptyList();
            }
            RecordActions recordActions = new RecordActions();
            recordActions.setRecord(info.getOriginalRecordRef().toString());
            recordActions.setActions(actions);
            resultList.add(recordActions);
        }

        RecordsQueryResult<Object> queryResult = new RecordsQueryResult<>();
        resultList.forEach(queryResult::addRecord);

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

        Map<Set<RecordRef>, List<RecordInfo>> recordsByActionsList = new HashMap<>();

        recordsInfo.forEach(info -> {
            if (info.getActionIds() != null && !info.getActionIds().isEmpty()) {
                Set<RecordRef> key = new HashSet<>(info.getActionIds());
                recordsByActionsList.computeIfAbsent(key, ids -> new ArrayList<>()).add(info);
            }
        });

        recordsByActionsList.forEach((actions, records) -> {

            List<RecordRef> refs = records.stream()
                .map(RecordInfo::getRecordRef)
                .collect(Collectors.toList());

            Map<RecordRef, List<ActionDto>> actionsRes = actionService.getActions(refs, new ArrayList<>(actions));

            records.forEach(info -> {

                List<ActionDto> recordActions = new ArrayList<>(actionsRes.get(info.getRecordRef()));

                recordActions.sort((a0, a1) -> {

                    List<RecordRef> recordActionIds = info.getActionIds();

                    RecordRef rec0 = RecordRef.create("uiserv", "action", a0.getId());
                    RecordRef rec1 = RecordRef.create("uiserv", "action", a1.getId());

                    int idx0 = recordActionIds.indexOf(rec0);
                    int idx1 = recordActionIds.indexOf(rec1);

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

    private List<ActionDto> expandResultActions(List<ActionDto> resultActions, List<ActionDto> recordActions) {
        if (resultActions == null) {
            return recordActions;
        }
        if (resultActions.isEmpty()) {
            return Collections.emptyList();
        }
        List<ActionDto> filteredActions = new ArrayList<>();
        resultActions.forEach(action -> {
            if ("record-actions".equals(action.getType())) {
                ObjectData config = action.getConfig();
                filteredActions.addAll(recordActions);
            } else {
                filteredActions.add(action);
            }
        });
        return filteredActions;
    }

    @Autowired(required = false)
    public void setRecordEventsService(RecordEventsService recordEventsService) {
        this.recordEventsService = recordEventsService;
    }

    @Data
    public static class RecordTypeMeta {
        @AttName("_type?id")
        private String type;
    }

    @Data
    public static class ActionsDtoMeta {
        @AttName("_actions[]")
        private List<RecordActionDto> actions;
    }

    @Data
    public static class RecordActionDto {
        private String id;
        private String name;
        private String key;
        private String icon;
        private String type;
        private ObjectData config = ObjectData.create();
    }

    @Data
    public static class ActionsRefMeta {
        @AttName("_actions[]?str")
        private List<RecordRef> actions;
    }

    @Data
    public static class ActionsQuery {
        private List<JsonNode> records;
        private List<RecordRef> actions;
    }

    @Data
    public static class RecordActions {

        private String id = UUID.randomUUID().toString();

        private String record;
        private List<ActionDto> actions;
    }

    @Data
    private static class RecordInfo {

        private final RecordRef recordRef;
        private final RecordRef originalRecordRef;

        private RecordRef type = RecordRef.EMPTY;

        private List<ActionDto> recordActions = Collections.emptyList();
        private List<ActionDto> resultActions = Collections.emptyList();

        private List<RecordRef> actionIds;

        public RecordInfo(JsonNode record) {

            if (record instanceof TextNode) {
                originalRecordRef = RecordRef.valueOf(record.asText());
            } else if (record instanceof ObjectNode) {
                originalRecordRef = RecordRef.valueOf(record.get("record").asText());
                if (record.has("actions")) {
                    actionIds = new ArrayList<>();
                    record.get("actions").forEach(action ->
                        actionIds.add(RecordRef.valueOf(action.asText()))
                    );
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

        private void setRecordActions(List<RecordActionDto> actions) {

            if (actions != null && actions.size() > 0) {

                recordActions = actions.stream().map(a -> {
                    ActionDto module = new ActionDto();
                    module.setId(a.id);
                    module.setName(createMLText(a.name));
                    module.setIcon(a.icon);
                    module.setType(a.type);
                    module.setConfig(a.config);
                    return module;
                }).collect(Collectors.toList());

            } else {

                recordActions = Collections.emptyList();
            }
        }

        private MLText createMLText(String text) {
            if (text == null) {
                text = "null";
            }
            return MLText.EMPTY.withValue(RequestContext.getLocale(), text);
        }
    }

    public class ActionRecord extends ActionDto {

        public ActionRecord(ActionDto model) {
            super(model);
        }

        public ActionRecord() {
        }

        public String getModuleId() {
            return getId();
        }

        public void setModuleId(String value) {
            setId(value);
        }

        @AttName("?disp")
        public String getDisplayName() {
            MLText mlName = getName();
            String name = mlName != null ? mlName.getClosestValue(QueryContext.getCurrent().getLocale()) : null;
            return StringUtils.defaultString(name, "Action");
        }

        public RecordRef getEcosType() {
            RecordRef typeRef = TypeUtils.getTypeRef("ui-action/" + this.getType());
            if (typesRepo.getTypeInfo(typeRef) != null) {
                return typeRef;
            }
            return TypeUtils.getTypeRef("ui-action");
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String dataUriContent = content.get(0).get("url", "");
            ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);

            Json.getMapper().applyData(this, data);
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        public ActionDto toJson() {
            return new ActionDto(this);
        }

        public byte[] getData() {
            return YamlUtils.toNonDefaultString(toJson()).getBytes(StandardCharsets.UTF_8);
        }
    }
}
