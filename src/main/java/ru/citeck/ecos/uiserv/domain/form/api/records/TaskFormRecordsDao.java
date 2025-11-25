package ru.citeck.ecos.uiserv.domain.form.api.records;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.context.lib.i18n.I18nContext;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.element.elematts.RecordAttsElement;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;
import ru.citeck.ecos.uiserv.domain.form.service.FormDefUtils;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskFormRecordsDao extends AbstractRecordsDao
    implements RecordsQueryDao {

    private static final String OUTCOME_PREFIX = "outcome_";
    private static final String LANG_TASKS = "tasks";
    private static final String LANG_FORM = "form";
    private static final String WF_TASK_SOURCE_ID = "wftask";

    private final EcosFormService formService;
    private final RecordsServiceFactory recordsServices;
    private final PredicateService predicateService;
    private final WorkspaceService workspaceService;

    private Map<String, String> taskInfoAttsToLoad;

    @PostConstruct
    public void init() {
        taskInfoAttsToLoad = recordsServices.getAttSchemaWriter().writeToMap(
            recordsServices.getDtoSchemaReader().read(TaskInfo.class)
        );
    }

    @Nullable
    @Override
    public Object queryRecords(@NotNull RecordsQuery recsQuery) throws Exception {

        String language = recsQuery.getLanguage();
        if (StringUtils.isBlank(language)) {
            return new RecsQueryRes<>();
        }

        List<?> result = Collections.emptyList();

        switch (language) {
            case LANG_TASKS:
                result = queryTasks(recsQuery.getQuery(ActionsQuery.class));
                break;
            case LANG_FORM:
                result = Collections.singletonList(queryForm(recsQuery.getQuery(FormQuery.class)));
                break;
        }

        @SuppressWarnings("unchecked")
        List<Object> typedRes = (List<Object>) result;
        return new RecsQueryRes<>(typedRes);
    }

    @NotNull
    private EcosFormDef queryForm(FormQuery formQuery) {

        if (formQuery == null || EntityRef.isEmpty(formQuery.formRef)) {
            return EcosFormDef.create().build();
        }

        IdInWs idInWs = workspaceService.convertToIdInWs(formQuery.getFormRef().getLocalId());
        EcosFormDef formById = formService.getFormById(idInWs).orElse(null);
        if (formById == null) {
            return EcosFormDef.create().build();
        }

        EcosFormDef.Builder result = formById.copy();
        DataValue newDef = removeOutcomes(result.getDefinition().getData());
        result.withDefinition(newDef.isNotNull() ? newDef.asObjectData() : ObjectData.create());

        return result.build();
    }

    private List<RecordTaskActionsInfo> queryTasks(ActionsQuery actionsQuery) {

        if (actionsQuery == null || actionsQuery.recordRefs == null || actionsQuery.recordRefs.isEmpty()) {
            return Collections.emptyList();
        }

        return actionsQuery.getRecordRefs().stream().map(ref ->
            new RecordTaskActionsInfo(ref, queryTasks(ref, actionsQuery.evalOutcomesForTasks))
        ).collect(Collectors.toList());
    }

    private List<TaskActionsInfo> queryTasks(EntityRef recordRef, Predicate evalOutcomesForTasks) {

        if (recordRef == null || EntityRef.isEmpty(recordRef)) {
            return Collections.emptyList();
        }

        TasksQuery tasksQuery = new TasksQuery();
        tasksQuery.setActive(true);
        tasksQuery.setActor("$CURRENT");
        tasksQuery.setDocument(recordRef.toString());

        RecordsQuery tasksRecsQuery = RecordsQuery.create()
            .withSourceId(AppName.EPROC + "/" + WF_TASK_SOURCE_ID)
            .withQuery(tasksQuery)
            .build();

        Set<String> attsToLoad = new HashSet<>(taskInfoAttsToLoad.values());
        attsToLoad.addAll(PredicateUtils.getAllPredicateAttributes(evalOutcomesForTasks));

        List<RecordAtts> tasksAtts = recordsService.query(tasksRecsQuery, attsToLoad).getRecords();

        if (tasksAtts.isEmpty()) {
            return Collections.emptyList();
        }

        List<TaskActionsInfo> actions = new ArrayList<>();

        for (RecordAtts taskAtts : tasksAtts) {

            ObjectData taskInfoDtoAtts = ObjectData.create();
            taskInfoAttsToLoad.forEach((k, v) -> taskInfoDtoAtts.set(k, taskAtts.get(v)));
            TaskInfo task = recordsServices.getDtoSchemaReader().instantiate(TaskInfo.class, taskInfoDtoAtts);
            if (task == null) {
                continue;
            }

            EcosFormDef form = getEcosFormDef(task);

            if (form == null || form.getDefinition().isEmpty()) {
                continue;
            }

            List<Outcome> outcomes;
            if (predicateService.isMatch(RecordAttsElement.create(taskAtts), evalOutcomesForTasks)) {
                outcomes = getOutcomesForForm(form, task.possibleOutcomes);
            } else {
                outcomes = Collections.emptyList();
            }

            String taskId = task.getId();
            if (StringUtils.isNotBlank(taskId)) {
                EntityRef taskRef = EntityRef.valueOf(taskId);
                if (taskRef.getAppName().isEmpty()) {
                    taskRef = taskRef.withAppName(AppName.EPROC);
                }

                if (taskRef.getSourceId().isEmpty()) {
                    taskRef = taskRef.withSourceId(WF_TASK_SOURCE_ID);
                }

                EntityRef formRef = EntityRef.create(AppName.UISERV, EcosFormRecordsDao.ID, form.getId());
                actions.add(new TaskActionsInfo(task.getTaskDisp(), taskRef, formRef, outcomes));
            } else {
                log.warn("Strange task: {}", task);
            }
        }

        return actions;
    }

    private EcosFormDef getEcosFormDef(TaskInfo task) {
        EcosFormDef form = null;

        if (task.getFormRef() != null && task.getFormRef().isNotEmpty()) {
            IdInWs idInWs = workspaceService.convertToIdInWs(task.getFormRef().getLocalId());
            form = formService.getFormById(idInWs).orElse(null);
        }

        if (form == null) {
            form = formService.getFormByKey(task.getFormKey()).orElse(null);
        }

        return form;
    }

    @NotNull
    private DataValue removeOutcomes(DataValue definition) {

        if (!definition.isObject()) {
            return definition;
        }

        if (isOutcomeBtn(definition) || isTaskOutcomeComponent(definition)) {
            return DataValue.NULL;
        }

        DataValue resultComponent = definition;
        DataValue components = FormDefUtils.getInnerComponents(definition);

        if (components.isArray()) {

            resultComponent = resultComponent.copy();
            DataValue resultArr = DataValue.createArr();

            for (DataValue value : components) {
                DataValue newValue = removeOutcomes(value);
                if (newValue.isNotNull()) {
                    resultArr.add(newValue);
                }
            }

            if (resultArr.isEmpty()) {
                return DataValue.NULL;
            }
            setInnerComponents(resultComponent, resultArr);
        }

        return resultComponent;
    }

    private List<Outcome> getOutcomesForForm(
        @NotNull EcosFormDef form,
        List<PossibleOutcome> possibleOutcomes
    ) {

        ObjectData i18n = form.getI18n();
        if (i18n.isNotEmpty()) {
            Locale locale = I18nContext.getLocale();
            DataValue messages = i18n.get(locale.getLanguage());
            if (messages.isNotNull()) {
                i18n = messages.asObjectData();
            } else {
                Iterator<String> names = i18n.fieldNames();
                if (names.hasNext()) {
                    i18n = i18n.get(names.next()).asObjectData();
                }
            }
        }

        return getOutcomes(form.getDefinition().getData(), i18n, possibleOutcomes);
    }

    private List<Outcome> getOutcomes(
        DataValue definition,
        ObjectData i18n,
        List<PossibleOutcome> possibleOutcomes
    ) {
        ArrayList<Outcome> result = new ArrayList<>();
        getOutcomes(definition, result, i18n, possibleOutcomes);
        return result;
    }

    private void getOutcomes(
        DataValue definition,
        List<Outcome> result,
        ObjectData i18n,
        List<PossibleOutcome> possibleOutcomes
    ) {

        if (!definition.isObject()) {
            return;
        }

        if (isTaskOutcomeComponent(definition)) {
            for (PossibleOutcome possibleOutcome : possibleOutcomes) {
                if (StringUtils.isBlank(possibleOutcome.id)) {
                    continue;
                }
                String label = StringUtils.defaultIfBlank(possibleOutcome.name, possibleOutcome.id);
                result.add(new Outcome(label, possibleOutcome.id));
            }
            return;
        }

        if (isOutcomeBtn(definition)) {

            String key = definition.get("key").asText();

            DataValue labelData = definition.get("label");
            String label;
            if (labelData.isTextual()) {
                label = labelData.asText();
            } else {
                MLText mlLabel = Json.getMapper().convert(labelData, MLText.class);
                label = mlLabel != null ? mlLabel.getClosestValue(I18nContext.getLocale()) : "";
            }

            if (!label.isEmpty()) {
                DataValue msg = i18n.get(label);
                if (msg.isNotNull() && !msg.asText().isEmpty()) {
                    label = msg.asText();
                }
            }

            result.add(new Outcome(label, key.substring(OUTCOME_PREFIX.length())));
        }

        DataValue components = FormDefUtils.getInnerComponents(definition);
        if (components.isArray()) {
            for (DataValue value : components) {
                getOutcomes(value, result, i18n, possibleOutcomes);
            }
        }
    }

    private boolean isTaskOutcomeComponent(DataValue value) {
        String type = value.get("type").asText();
        return type.equals("taskOutcome");
    }

    private boolean isOutcomeBtn(DataValue value) {

        String type = value.get("type").asText();
        String key = value.get("key").asText();

        return type.equals("button") && key.startsWith(OUTCOME_PREFIX);
    }

    private void setInnerComponents(DataValue component, DataValue inner) {
        if (component.get("type").asText().equals("columns")) {
            component.set("columns", inner);
        } else {
            component.set("components", inner);
        }
    }

    @NotNull
    @Override
    public String getId() {
        return "task-form";
    }

    @Data
    @AllArgsConstructor
    private static class Outcome {
        private String label;
        private String outcome;
    }

    @Data
    @AllArgsConstructor
    public static class RecordTaskActionsInfo {
        private EntityRef recordRef;
        private List<TaskActionsInfo> taskActions;
    }

    @Data
    @AllArgsConstructor
    public static class TaskActionsInfo {
        private String taskDisp;
        @NotNull
        private EntityRef taskRef;
        @NotNull
        private EntityRef formRef;
        @NotNull
        private List<Outcome> outcomes;
    }

    @Data
    public static class TaskInfo {
        private String id;
        @AttName("?disp")
        private String taskDisp;
        @AttName("_formKey?str")
        private String formKey;

        @AttName("_formRef")
        private EntityRef formRef;
        @AttName("possibleOutcomes![]")
        private List<PossibleOutcome> possibleOutcomes;
    }

    @Data
    public static class PossibleOutcome {
        @AttName("id!")
        private String id;
        @AttName("name!")
        private String name;
    }

    @Data
    public static class TasksQuery {
        private String actor;
        private Boolean active;
        private String document;
    }

    @Data
    public static class FormQuery {
        private EntityRef formRef;
    }

    @Data
    public static class ActionsQuery {
        private List<EntityRef> recordRefs;
        private Predicate evalOutcomesForTasks = Predicates.alwaysTrue();
    }
}
