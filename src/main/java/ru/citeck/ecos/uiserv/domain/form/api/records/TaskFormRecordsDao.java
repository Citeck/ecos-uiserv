package ru.citeck.ecos.uiserv.domain.form.api.records;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;
import ru.citeck.ecos.uiserv.domain.form.service.FormDefUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskFormRecordsDao extends LocalRecordsDao
                                implements LocalRecordsQueryWithMetaDao<Object> {

    private static final String OUTCOME_PREFIX = "outcome_";
    private static final String LANG_TASKS = "tasks";
    private static final String LANG_FORM = "form";

    private final EcosFormService formService;

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(@NotNull RecordsQuery query,
                                                        @NotNull MetaField field) {

        String language = query.getLanguage();
        if (StringUtils.isBlank(language)) {
            return new RecordsQueryResult<>();
        }

        List<?> result = Collections.emptyList();

        switch (language) {
            case LANG_TASKS:
                result = queryTasks(query.getQuery(ActionsQuery.class));
                break;
            case LANG_FORM:
                result = Collections.singletonList(queryForm(query.getQuery(FormQuery.class)));
                break;
        }

        @SuppressWarnings("unchecked")
        List<Object> typedRes = (List<Object>) result;
        return new RecordsQueryResult<>(typedRes);
    }

    @NotNull
    private EcosFormDef queryForm(FormQuery formQuery) {

        if (formQuery == null || RecordRef.isEmpty(formQuery.formRef)) {
            return EcosFormDef.create().build();
        }

        EcosFormDef formById = formService.getFormById(formQuery.getFormRef().getId()).orElse(null);
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

    private List<TaskActionsInfo> queryTasks(RecordRef recordRef) {

        if (recordRef == null || RecordRef.isEmpty(recordRef)) {
            return Collections.emptyList();
        }

        RecordsQuery tasksRecsQuery = new RecordsQuery();
        tasksRecsQuery.setSourceId("alfresco/wftask");

        TasksQuery tasksQuery = new TasksQuery();
        tasksQuery.setActive(true);
        tasksQuery.setActor("$CURRENT");
        tasksQuery.setDocument(recordRef.toString());
        tasksRecsQuery.setQuery(tasksQuery);

        RecordsQueryResult<TaskInfo> tasks = recordsService.queryRecords(tasksRecsQuery, TaskInfo.class);

        List<TaskInfo> currentTasks = tasks.getRecords();

        if (currentTasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<TaskActionsInfo> actions = new ArrayList<>();

        for (TaskInfo task : currentTasks) {

            EcosFormDef form = formService.getFormByKey(task.getFormKey()).orElse(null);
            if (form == null || form.getDefinition().isEmpty()) {
                continue;
            }

            ObjectData i18n = form.getI18n();
            if (i18n.isNotEmpty()) {
                Locale locale = QueryContext.getCurrent().getLocale();
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
                if (!taskId.startsWith("wftask") && !taskId.startsWith("alfresco")) {
                    taskId = "wftask@" + taskId;
                }
                RecordRef taskRef = RecordRef.valueOf(taskId);
                RecordRef formRef = RecordRef.create("uiserv", EcosFormRecordsDao.ID, form.getId());
                actions.add(new TaskActionsInfo(task.getTaskDisp(), taskRef, formRef, outcomes));
            } else {
                log.warn("Strange task: " + task);
            }
        }

        return actions;
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

            if (resultArr.size() == 0) {
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
            String label = definition.get("label").asText();
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
        private RecordRef recordRef;
        private List<TaskActionsInfo> taskActions;
    }

    @Data
    @AllArgsConstructor
    public static class TaskActionsInfo {
        private String taskDisp;
        @NotNull
        private RecordRef taskRef;
        @NotNull
        private RecordRef formRef;
        @NotNull
        private List<Outcome> outcomes;
    }

    @Data
    public static class TaskInfo {
        private String id;
        @AttName(".disp")
        private String taskDisp;
        @AttName("_formKey?str")
        private String formKey;
    }

    @Data
    public static class TasksQuery {
        private String actor;
        private Boolean active;
        private String document;
    }

    @Data
    public static class FormQuery {
        private RecordRef formRef;
    }

    @Data
    public static class ActionsQuery {
        private List<RecordRef> recordRefs;
    }
}
