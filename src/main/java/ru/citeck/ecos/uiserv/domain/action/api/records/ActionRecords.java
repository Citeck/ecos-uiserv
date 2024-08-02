package ru.citeck.ecos.uiserv.domain.action.api.records;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import kotlin.Unit;
import lombok.Data;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.context.lib.i18n.I18nContext;
import ru.citeck.ecos.events2.type.RecordEventsService;
import ru.citeck.ecos.model.lib.type.repo.TypesRepo;
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.uiserv.domain.action.service.ActionService;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Component
public class ActionRecords extends AbstractRecordsDao
    implements RecordsQueryDao,
    RecordAttsDao,
    RecordMutateDtoDao<ActionRecords.ActionRecord>, RecordsDeleteDao {

    private static final String RECORD_ACTIONS_TYPE = "record-actions";

    public static final String ID = "action";

    private final ActionService actionService;
    private RecordEventsService recordEventsService;
    private final TypesRepo typesRepo;

    @Autowired
    public ActionRecords(ActionService actionService, TypesRepo typesRepo) {
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

    @NotNull
    @Override
    public List<DelStatus> delete(@NotNull List<String> records) throws Exception {
        records.forEach(actionService::deleteAction);
        return records.stream().map(r -> DelStatus.OK).toList();
    }

    @Override
    public ActionRecord getRecToMutate(@NotNull String recordId) throws Exception {
        Object recordAtts = getRecordAtts(recordId);
        if (!(recordAtts instanceof ActionRecord)) {
            throw new RuntimeException("Action doesn't found: '" + recordId + "'");
        }
        return (ActionRecord) recordAtts;
    }

    @NotNull
    @Override
    public String saveMutatedRec(ActionRecord actionRecord) throws Exception {
        actionService.updateAction(actionRecord);
        return actionRecord.getId();
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {

        if (recordId.isEmpty()) {
            return new ActionRecord();
        }
        ActionDto action = actionService.getAction(recordId);
        if (action == null) {
            return EmptyAttValue.INSTANCE;
        }
        if (action instanceof ActionRecord) {
            return action;
        }
        return new ActionRecord(action);
    }

    @Nullable
    @Override
    public Object queryRecords(@NotNull RecordsQuery recordsQuery) throws Exception {

        ActionsQuery parsedQuery = null;
        if (StringUtils.isBlank(recordsQuery.getLanguage())) {
            parsedQuery = recordsQuery.getQuery(ActionsQuery.class);
        }

        if (parsedQuery == null) {

            RecsQueryRes<Object> result = new RecsQueryRes<>();

            int max = recordsQuery.getPage().getMaxItems();
            int skip = recordsQuery.getPage().getSkipCount();

            List<ActionDto> actions;

            if (PredicateService.LANGUAGE_PREDICATE.equals(recordsQuery.getLanguage())) {

                Predicate predicate = recordsQuery.getQuery(Predicate.class);

                actions = actionService.getActions(
                    predicate,
                    max,
                    skip,
                    recordsQuery.getSortBy()
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

        RecsQueryRes<Object> queryResult = new RecsQueryRes<>();
        resultList.forEach(queryResult::addRecord);

        return queryResult;
    }

    private void fillRecordsType(List<RecordInfo> recordsInfo) {

        List<EntityRef> recordRefs = recordsInfo.stream().map(RecordInfo::getRecordRef).collect(Collectors.toList());

        List<RecordTypeMeta> recordTypes = recordsService.getAtts(recordRefs, RecordTypeMeta.class);

        for (int i = 0; i < recordTypes.size(); i++) {
            String type = recordTypes.get(i).type;
            if (i < recordsInfo.size() && StringUtils.isNotBlank(type)) {
                recordsInfo.get(i).setType(EntityRef.valueOf(type));
            }
        }
    }

    private void expandActionIds(List<RecordInfo> recordsInfo) {

        Map<Set<EntityRef>, List<RecordInfo>> recordsByActionsList = new HashMap<>();

        recordsInfo.forEach(info -> {
            if (info.getActionIds() != null && !info.getActionIds().isEmpty()) {
                Set<EntityRef> key = new HashSet<>(info.getActionIds());
                recordsByActionsList.computeIfAbsent(key, ids -> new ArrayList<>()).add(info);
            }
        });

        recordsByActionsList.forEach((actions, records) -> {

            List<EntityRef> refs = records.stream()
                .map(RecordInfo::getRecordRef)
                .collect(Collectors.toList());

            Map<EntityRef, List<ActionDto>> actionsRes = actionService.getActions(refs, new ArrayList<>(actions));

            records.forEach(info -> {

                List<ActionDto> recordActions = new ArrayList<>(actionsRes.get(info.getRecordRef()));

                recordActions.sort((a0, a1) -> {

                    List<EntityRef> recordActionIds = info.getActionIds();

                    EntityRef rec0 = EntityRef.create("uiserv", "action", a0.getId());
                    EntityRef rec1 = EntityRef.create("uiserv", "action", a1.getId());

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

        Map<EntityRef, List<RecordInfo>> recordsByType = new HashMap<>();

        for (RecordInfo info : recordsInfo) {
            EntityRef type = info.getType();
            if (type != null) {
                recordsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(info);
            }
        }

        List<EntityRef> typesRefs = new ArrayList<>(recordsByType.keySet());
        List<ActionsRefMeta> typesActions = recordsService.getAtts(typesRefs, ActionsRefMeta.class);

        for (int i = 0; i < typesActions.size(); i++) {
            ActionsRefMeta typeActions = typesActions.get(i);
            EntityRef typeRef = typesRefs.get(i);
            recordsByType.get(typeRef).forEach(info -> info.setActionIds(typeActions.getActions()));
        }
    }

    private void fillRecordActions(List<RecordInfo> recordsInfo) {

        List<EntityRef> recordsToQueryActions = recordsInfo.stream().filter(info ->
            info.getActionIds() == null
                || info.getResultActions() == null
                || info.getResultActions()
                .stream()
                .anyMatch(a -> RECORD_ACTIONS_TYPE.equals(a.getType()))
        ).map(RecordInfo::getRecordRef).collect(Collectors.toList());

        List<ActionsDtoMeta> actions = recordsService.getAtts(recordsToQueryActions, ActionsDtoMeta.class);

        for (int i = 0; i < actions.size(); i++) {
            if (i < recordsToQueryActions.size()) {
                EntityRef recordRef = recordsToQueryActions.get(i);
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

    @NotNull
    @Override
    public String getId() {
        return ID;
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
        private List<EntityRef> actions;
    }

    @Data
    public static class ActionsQuery {
        private List<JsonNode> records;
        private List<EntityRef> actions;
    }

    @Data
    public static class RecordActions {

        private String id = UUID.randomUUID().toString();

        private String record;
        private List<ActionDto> actions;
    }

    @Data
    private static class RecordInfo {

        private final EntityRef recordRef;
        private final EntityRef originalRecordRef;

        private EntityRef type = EntityRef.EMPTY;

        private List<ActionDto> recordActions = Collections.emptyList();
        private List<ActionDto> resultActions = Collections.emptyList();

        private List<EntityRef> actionIds;

        public RecordInfo(JsonNode record) {

            if (record instanceof TextNode) {
                originalRecordRef = EntityRef.valueOf(record.asText());
            } else if (record instanceof ObjectNode) {
                originalRecordRef = EntityRef.valueOf(record.get("record").asText());
                if (record.has("actions")) {
                    actionIds = new ArrayList<>();
                    record.get("actions").forEach(action ->
                        actionIds.add(EntityRef.valueOf(action.asText()))
                    );
                }
            } else {
                throw new IllegalArgumentException("Incorrect record info: " + record);
            }

            if (!originalRecordRef.getAppName().isEmpty()) {
                recordRef = originalRecordRef;
            } else {
                recordRef = originalRecordRef.withAppName("alfresco");
            }
        }

        private void setRecordActions(List<RecordActionDto> actions) {

            if (actions != null && !actions.isEmpty()) {

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
            return MLText.EMPTY.withValue(I18nContext.getLocale(), text);
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
            String name = mlName != null ? mlName.getClosestValue(I18nContext.getLocale()) : null;
            return StringUtils.defaultString(name, "Action");
        }

        public EntityRef getEcosType() {
            EntityRef typeRef = TypeUtils.getTypeRef("ui-action/" + this.getType());
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
        public ActionDto toJson() {
            return new ActionDto(this);
        }

        public byte[] getData() {
            return YamlUtils.toNonDefaultString(toJson()).getBytes(StandardCharsets.UTF_8);
        }
    }
}
