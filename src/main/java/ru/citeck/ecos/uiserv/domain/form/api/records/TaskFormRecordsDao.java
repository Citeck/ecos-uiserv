package ru.citeck.ecos.uiserv.domain.form.api.records;

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

        EcosFormDef formById = formService.getFormById(formQuery.getFormRef().getLocalId()).orElse(null);
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
            new RecordTaskActionsInfo(ref, queryTasks(ref))
        ).collect(Collectors.toList());
    }

    private List<TaskActionsInfo> queryTasks(EntityRef recordRef) {

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

        RecsQueryRes<TaskInfo> tasks = recordsService.query(tasksRecsQuery, TaskInfo.class);

        List<TaskInfo> currentTasks = tasks.getRecords();

        if (currentTasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<TaskActionsInfo> actions = new ArrayList<>();

        for (TaskInfo task : currentTasks) {

            EcosFormDef form = getEcosFormDef(task);

            if (form == null || form.getDefinition().isEmpty()) {
                continue;
            }

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

            List<Outcome> outcomes = getOutcomes(form.getDefinition().getData(), i18n);
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
            form = formService.getFormById(task.getFormRef().getLocalId()).orElse(null);
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

        if (isOutcomeBtn(definition)) {
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

    private List<Outcome> getOutcomes(DataValue definition, ObjectData i18n) {
        ArrayList<Outcome> result = new ArrayList<>();
        getOutcomes(definition, result, i18n);
        return result;
    }

    private void getOutcomes(DataValue definition, List<Outcome> result, ObjectData i18n) {

        if (!definition.isObject()) {
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
                getOutcomes(value, result, i18n);
            }
        }
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
    }
}
