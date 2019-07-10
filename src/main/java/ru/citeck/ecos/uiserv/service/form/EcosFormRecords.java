package ru.citeck.ecos.uiserv.service.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.annotation.DisplayName;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.CrudRecordsDAO;
import ru.citeck.ecos.uiserv.domain.EcosFormModel;

import java.util.*;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class EcosFormRecords extends CrudRecordsDAO<EcosFormRecords.EcosFormModelDownstream> {

    public static final String ID = "eform";

    private static final String ECOS_FORM_KEY = "ECOS_FORM";

    private static final RecordRef DEFAULT_FORM_ID = RecordRef.create(ID, "DEFAULT");
    private static final RecordRef ECOS_FORM_ID = RecordRef.create(ID, "ECOS_FORM");

    private static final Set<RecordRef> SYSTEM_FORMS = new HashSet<>(Arrays.asList(DEFAULT_FORM_ID, ECOS_FORM_ID));

    private final EcosFormService eformFormService; //рекордам нужен сервис, сервис создается конфигом и потому для его создания нужен конфиг, а конфигу нужны рекорды
    private final MessageSource messageSource;

    {
        setId(ID);
    }

    @Override
    public List<EcosFormModelDownstream> getValuesToMutate(List<RecordRef> records) {
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
        final String displayName = model.getTitle() != null ? model.getTitle() :
            messageSource.getMessage("ecosForms_model.type.ecosForms_form.title", null,
                LocaleContextHolder.getLocale());

        return new EcosFormModelDownstream(model, displayName);
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
    public List<EcosFormModelDownstream> getMetaValues(List<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(id -> Optional.of(id)
                .filter(str -> !str.isEmpty())
                .map(x -> eformFormService.getFormById(x)
                    .orElseThrow(() -> new IllegalArgumentException("Form with id " + id + " not found!")))
                .orElseGet(() -> {
                    final EcosFormModel form = new EcosFormModel();
                    form.setId("");
                    return form;
                }))
            .map(this::toDownstream)
            .collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<EcosFormModelDownstream> getMetaValues(RecordsQuery recordsQuery) {

        String lang = recordsQuery.getLanguage();
        RecordsQueryResult<EcosFormModelDownstream> result = new RecordsQueryResult<>();

        if (lang != null && !lang.isEmpty()) {
            throw new IllegalArgumentException("This records source does not support specifying search language");
        }

        Query query = recordsQuery.getQuery(Query.class);
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

        } else if (query.record != null) {

            if (ID.equals(query.record.getSourceId())) {

                form = eformFormService.getFormByKey(ECOS_FORM_KEY);

            } else {
                form = eformFormService.getFormByRecord(query.record, query.isViewMode);
            }
        }

        form
            .map(this::toDownstream)
            .map(Collections::singletonList)
            .ifPresent(list -> {
                result.setRecords(list);
                result.setTotalCount(list.size());
            });

        return result;
    }

    @Data
    static class Query {
        private String formKey;
        private List<String> formKeys;
        private RecordRef record;
        private Boolean isViewMode;
    }

    public static class EcosFormModelDownstream extends EcosFormModel {
        private String displayName;

        @DisplayName
        @JsonIgnore
        public String getDisplayName() {
            return displayName;
        }

        public EcosFormModelDownstream(EcosFormModel model, String displayName) {
            super(model);
            this.displayName = displayName;
        }
    }
}
