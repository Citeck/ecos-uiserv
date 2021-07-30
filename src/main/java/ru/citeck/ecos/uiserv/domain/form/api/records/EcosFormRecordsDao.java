package ru.citeck.ecos.uiserv.domain.form.api.records;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.VoidPredicate;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EcosFormRecordsDao extends AbstractRecordsDao
    implements RecordsQueryDao,
               RecordMutateDtoDao<EcosFormRecord>,
               RecordDeleteDao,
               RecordAttsDao {

    public static final String ID = "form";

    private static final String FORMS_FOR_TYPE_LANG = "forms-for-type";

    private static final String DEFAULT_FORM_ID = "DEFAULT";
    private static final String ECOS_FORM_ID = "ECOS_FORM";

    private static final Set<String> SYSTEM_FORMS = new HashSet<>(Arrays.asList(DEFAULT_FORM_ID, ECOS_FORM_ID));

    private final EcosFormService ecosFormService;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Secured({"ROLE_ADMIN"})
    @Override
    public EcosFormRecord getRecToMutate(@NotNull String formId) {
        return toRecord(Optional.of(formId)
            .filter(str -> !str.isEmpty())
            .map(x -> ecosFormService.getFormById(x)
                .orElseThrow(() -> new IllegalArgumentException("Form with id " + formId + " not found!")))
            .map(EcosFormModel::new) //defensive copy, even though getFormById probably creates new instance
            .orElseGet(EcosFormModel::new));
    }

    private EcosFormRecord toRecord(EcosFormModel model) {
        return new EcosFormRecord(model);
    }

    @NotNull
    @Override
    public String saveMutatedRec(EcosFormRecord ecosFormModelRecord) {
        return ecosFormService.save(ecosFormModelRecord);
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String formId) {

        if (SYSTEM_FORMS.contains(formId)) {
            return DelStatus.PROTECTED;
        }
        ecosFormService.delete(formId);
        return DelStatus.OK;
    }

    @Nullable
    @Override
    public EcosFormRecord getRecordAtts(@NotNull String formId) {
        return toRecord(Optional.of(formId)
            .filter(str -> !str.isEmpty())
            .map(x -> ecosFormService.getFormById(x)
                .orElseThrow(() -> new IllegalArgumentException("Form with id " + formId + " not found!")))
            .orElseGet(() -> {
                final EcosFormModel form = new EcosFormRecord(new EcosFormModel());
                form.setId("");
                return form;
            }));
    }

    @Nullable
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {

        RecsQueryRes<EcosFormRecord> result = new RecsQueryRes<>();

        Query query = null;
        if (StringUtils.isBlank(recordsQuery.getLanguage())) {

            query = recordsQuery.getQuery(Query.class);

        } else if (recordsQuery.getLanguage().equals(FORMS_FOR_TYPE_LANG)) {

            FormsForTypeQuery formsForTypeQuery = recordsQuery.getQuery(FormsForTypeQuery.class);
            if (RecordRef.isEmpty(formsForTypeQuery.typeRef)) {
                return result;
            }

            result.addRecords(ecosFormService.getAllFormsForType(formsForTypeQuery.getTypeRef()).stream()
                .map(this::toRecord)
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

            List<EcosFormRecord> forms = ecosFormService.getAllForms(predicate, max, skipCount)
                .stream()
                .map(this::toRecord)
                .collect(Collectors.toList());

            result.setRecords(forms);
            result.setTotalCount(ecosFormService.getCount());
            return result;
        }

        Optional<EcosFormModel> form = Optional.empty();

        if (CollectionUtils.isNotEmpty(query.formKeys)) {
            List<EcosFormRecord> formsByKeys = ecosFormService.getFormsByKeys(query.formKeys)
                .stream()
                .map(this::toRecord)
                .collect(Collectors.toList());

            result.setTotalCount(formsByKeys.size());
            result.setRecords(formsByKeys);
            return result;
        } else if (StringUtils.isNotBlank(query.formKey)) {

            form = ecosFormService.getFormByKey(Arrays.stream(query.formKey.split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList()));

        }

        form.map(this::toRecord)
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
}
