package ru.citeck.ecos.uiserv.domain.form.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.VoidPredicate;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.page.SkipPage;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsCrudDao;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EcosFormRecordsDao extends LocalRecordsCrudDao<EcosFormRecordsDao.EcosFormModelDownstream> {

    public static final String ID = "eform";

    private static final String FORMS_FOR_TYPE_LANG = "forms-for-type";

    private static final RecordRef DEFAULT_FORM_ID = RecordRef.create(ID, "DEFAULT");
    private static final RecordRef ECOS_FORM_ID = RecordRef.create(ID, "ECOS_FORM");

    private static final Set<RecordRef> SYSTEM_FORMS = new HashSet<>(Arrays.asList(DEFAULT_FORM_ID, ECOS_FORM_ID));

    private final EcosFormService eformFormService;

    {
        setId(ID);
    }

    @Override
    public List<EcosFormModelDownstream> getValuesToMutate(@NotNull List<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(id ->
                Optional.of(id)
                    .filter(str -> !str.isEmpty())
                    .map(x -> eformFormService.getFormById(x)
                        .orElseThrow(() -> new IllegalArgumentException("Form with id " + id + " not found!")))
                    .map(EcosFormModel::new) //defensive copy, even though getFormById probably creates new instance
                    .orElseGet(EcosFormModel::new))
            .map(this::toDownstream)
            .collect(Collectors.toList());
    }

    private EcosFormModelDownstream toDownstream(EcosFormModel model) {
        return new EcosFormModelDownstream(model);
    }

    @Override
    public RecordsMutResult save(List<EcosFormModelDownstream> values) {

        RecordsMutResult result = new RecordsMutResult();

        for (final EcosFormModel model : values) {
            result.addRecord(new RecordMeta(eformFormService.save(model)));
        }

        return result;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {

        List<RecordMeta> resultRecords = new ArrayList<>();

        deletion.getRecords()
            .stream()
            .filter(r -> !SYSTEM_FORMS.contains(r))
            .forEach(r -> {
                eformFormService.delete(r.getId());
                resultRecords.add(new RecordMeta(r));
            });

        RecordsDelResult result = new RecordsDelResult();
        result.setRecords(resultRecords);
        return result;
    }

    @Override
    public List<EcosFormModelDownstream> getLocalRecordsMeta(@NotNull List<RecordRef> records,
                                                             @NotNull MetaField metaField) {
        return records.stream()
            .map(RecordRef::getId)
            .map(id -> Optional.of(id)
                .filter(str -> !str.isEmpty())
                .map(x -> eformFormService.getFormById(x)
                    .orElseThrow(() -> new IllegalArgumentException("Form with id " + id + " not found!")))
                .orElseGet(() -> {
                    final EcosFormModel form = new EcosFormModelDownstream(new EcosFormModel());
                    form.setId("");
                    return form;
                }))
            .map(this::toDownstream)
            .collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<EcosFormModelDownstream> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                                         @NotNull MetaField field) {

        RecordsQueryResult<EcosFormModelDownstream> result = new RecordsQueryResult<>();

        Query query = null;
        if (StringUtils.isBlank(recordsQuery.getLanguage())) {

            query = recordsQuery.getQuery(Query.class);

        } else if (recordsQuery.getLanguage().equals(FORMS_FOR_TYPE_LANG)) {

            FormsForTypeQuery formsForTypeQuery = recordsQuery.getQuery(FormsForTypeQuery.class);
            if (RecordRef.isEmpty(formsForTypeQuery.typeRef)) {
                return result;

            }

            result.addRecords(eformFormService.getAllFormsForType(formsForTypeQuery.getTypeRef()).stream()
                .map(this::toDownstream)
                .collect(Collectors.toList())
            );
            return result;
        }

        if (query == null) {

            int max = 10;
            int skipCount = 0;

            SkipPage page = recordsQuery.getSkipPage();
            if (page != null) {
                max = page.getMaxItems();
                skipCount = page.getSkipCount();
            }

            Predicate predicate = VoidPredicate.INSTANCE;
            if (PredicateService.LANGUAGE_PREDICATE.equals(recordsQuery.getLanguage())) {
                predicate = recordsQuery.getQuery(Predicate.class);
            }

            List<EcosFormModelDownstream> forms = eformFormService.getAllForms(predicate, max, skipCount)
                .stream()
                .map(this::toDownstream)
                .collect(Collectors.toList());

            result.setRecords(forms);
            result.setTotalCount(eformFormService.getCount());
            return result;
        }

        Optional<EcosFormModel> form = Optional.empty();

        if (CollectionUtils.isNotEmpty(query.formKeys)) {
            List<EcosFormModelDownstream> formsByKeys = eformFormService.getFormsByKeys(query.formKeys)
                .stream()
                .map(this::toDownstream)
                .collect(Collectors.toList());

            result.setTotalCount(formsByKeys.size());
            result.setRecords(formsByKeys);
            return result;
        } else if (StringUtils.isNotBlank(query.formKey)) {

            form = eformFormService.getFormByKey(Arrays.stream(query.formKey.split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList()));

        }

        form.map(this::toDownstream)
            .map(Collections::singletonList)
            .ifPresent(list -> {
                result.setRecords(list);
                result.setTotalCount(list.size());
            });

        return result;
    }

    @Data
    public static class Query {
        private String formKey;
        private List<String> formKeys;
        private RecordRef record;
        private Boolean isViewMode;
    }

    @Data
    public static class FormsForTypeQuery {
        private RecordRef typeRef;
    }

    public static class EcosFormModelDownstream extends EcosFormModel {


        public EcosFormModelDownstream(EcosFormModel model) {
            super(model);
        }

        public String getModuleId() {
            return getId();
        }

        public void setModuleId(String value) {
            setId(value);
        }

        @MetaAtt(".disp")
        public String getDisplayName() {
            String name = MLText.getClosestValue(getTitle(), QueryContext.getCurrent().getLocale());
            if (StringUtils.isBlank(name)) {
                name = "Form";
            }
            return name;
        }

        @JsonIgnore
        public String get_formKey() {
            return "module_form";
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String base64Content = content.get(0).get("url", "");
            base64Content = base64Content.replaceAll("^data:application/json;base64,", "");
            ObjectData data = Json.getMapper().read(Base64.getDecoder().decode(base64Content), ObjectData.class);

            Json.getMapper().applyData(this, data);
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        public EcosFormModel toJson() {
            return new EcosFormModel(this);
        }
    }
}
