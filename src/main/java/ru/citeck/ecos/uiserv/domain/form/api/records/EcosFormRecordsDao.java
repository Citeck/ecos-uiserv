package ru.citeck.ecos.uiserv.domain.form.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.VoidPredicate;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.delete.dao.RecordDeleteDao;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.op.query.dto.query.QueryPage;
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EcosFormRecordsDao extends AbstractRecordsDao
    implements RecordsQueryDao,
               RecordMutateDtoDao<EcosFormRecordsDao.EcosFormModelDownstream>,
               RecordDeleteDao,
               RecordAttsDao {

    public static final String ID = "eform";

    private static final String FORMS_FOR_TYPE_LANG = "forms-for-type";

    private static final String DEFAULT_FORM_ID = "DEFAULT";
    private static final String ECOS_FORM_ID = "ECOS_FORM";

    private static final Set<String> SYSTEM_FORMS = new HashSet<>(Arrays.asList(DEFAULT_FORM_ID, ECOS_FORM_ID));

    private final EcosFormService eformFormService;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public EcosFormModelDownstream getRecToMutate(@NotNull String formId) {
        return toDownstream(Optional.of(formId)
            .filter(str -> !str.isEmpty())
            .map(x -> eformFormService.getFormById(x)
                .orElseThrow(() -> new IllegalArgumentException("Form with id " + formId + " not found!")))
            .map(EcosFormModel::new) //defensive copy, even though getFormById probably creates new instance
            .orElseGet(EcosFormModel::new));
    }

    private EcosFormModelDownstream toDownstream(EcosFormModel model) {
        return new EcosFormModelDownstream(model);
    }

    @NotNull
    @Override
    public String saveMutatedRec(EcosFormModelDownstream ecosFormModelDownstream) {
        return eformFormService.save(ecosFormModelDownstream);
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String formId) {

        if (SYSTEM_FORMS.contains(formId)) {
            return DelStatus.PROTECTED;
        }
        eformFormService.delete(formId);
        return DelStatus.OK;
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String formId) {
        return toDownstream(Optional.of(formId)
            .filter(str -> !str.isEmpty())
            .map(x -> eformFormService.getFormById(x)
                .orElseThrow(() -> new IllegalArgumentException("Form with id " + formId + " not found!")))
            .orElseGet(() -> {
                final EcosFormModel form = new EcosFormModelDownstream(new EcosFormModel());
                form.setId("");
                return form;
            }));
    }

    @Nullable
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {

        RecsQueryRes<EcosFormModelDownstream> result = new RecsQueryRes<>();

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

            QueryPage page = recordsQuery.getPage();
            int max = page.getMaxItems();
            int skipCount = page.getSkipCount();

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

        @AttName(".disp")
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
