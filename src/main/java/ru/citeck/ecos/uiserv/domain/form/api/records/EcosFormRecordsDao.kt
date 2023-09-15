package ru.citeck.ecos.uiserv.domain.form.api.records;

import kotlin.Unit;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.entity.EntityWithMeta;
import ru.citeck.ecos.context.lib.auth.AuthRole;
import ru.citeck.ecos.events2.type.RecordEventsService;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.AttributePredicate;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.VoidPredicate;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;
import ru.citeck.ecos.uiserv.domain.form.registry.FormsRegistryConfiguration;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;
import ru.citeck.ecos.uiserv.domain.form.service.provider.TypeFormsProvider;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EcosFormRecordsDao extends AbstractRecordsDao
    implements RecordsQueryDao,
    RecordMutateDtoDao<EcosFormMutRecord>,
    RecordDeleteDao,
    RecordAttsDao {

    public static final String ID = "form";

    private static final String FORMS_FOR_TYPE_LANG = "forms-for-type";

    // form with default content for new forms
    private static final String DEFAULT_FORM_ID = "DEFAULT";
    // form to create and edit any form metadata and definition
    private static final String ECOS_FORM_ID = "ECOS_FORM";

    private static final String DEFAULT_AUTO_FORM_FOR_TYPE = "DEFAULT_FORM";

    private static final Map<String, String> ATTS_MAPPING;

    public static final Set<String> SYSTEM_FORMS = new HashSet<>(Arrays.asList(
        DEFAULT_AUTO_FORM_FOR_TYPE,
        DEFAULT_FORM_ID,
        ECOS_FORM_ID
    ));

    static {
        ATTS_MAPPING = new HashMap<>();
        ATTS_MAPPING.put("moduleId", "id");
    }

    private final EcosFormService ecosFormService;
    @Nullable
    private final TypeFormsProvider typeFormsProvider;

    private RecordEventsService recordEventsService;

    @PostConstruct
    public void init() {
        ecosFormService.addChangeListener((before, after) -> {
            if (recordEventsService != null) {
                recordEventsService.emitRecChanged(before, after, getId(), EcosFormRecord::new);
            }
        });
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Secured({ AuthRole.ADMIN })
    @Override
    public EcosFormMutRecord getRecToMutate(@NotNull String formId) {
        if (formId.isEmpty()) {
            return new EcosFormMutRecord();
        }
        Optional<EcosFormDef> currentForm = ecosFormService.getFormById(formId);
        if (!currentForm.isPresent()) {
            throw new IllegalArgumentException("Form with id " + formId + " not found!");
        }
        return new EcosFormMutRecord(currentForm.get());
    }

    private EcosFormRecord toRecord(EcosFormDef model) {
        return new EcosFormRecord(model);
    }

    @NotNull
    @Override
    public String saveMutatedRec(EcosFormMutRecord record) {
        return ecosFormService.save(record.build());
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
        if (formId.isEmpty()) {
            return toRecord(EcosFormDef.create().build());
        }
        return ecosFormService.getFormById(formId)
            .map(this::toRecord)
            .orElse(null);
    }

    @Nullable
    @Override
    public RecsQueryRes<EcosFormRecord> queryRecords(@NotNull RecordsQuery recordsQuery) {

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

            if (PredicateService.LANGUAGE_PREDICATE.equals(recordsQuery.getLanguage())) {

                Predicate predicate = PredicateUtils.mapAttributePredicates(
                    recordsQuery.getQuery(Predicate.class),
                    pred -> {
                        if (ATTS_MAPPING.containsKey(pred.getAttribute())) {
                            AttributePredicate copy = pred.copy();
                            copy.setAttribute(ATTS_MAPPING.get(pred.getAttribute()));
                            return copy;
                        } else {
                            return pred;
                        }
                    }
                );

                List<SortBy> mappedSortBy = recordsQuery.getSortBy().stream().map(it -> {
                    if (ATTS_MAPPING.containsKey(it.getAttribute())) {
                        return it.copy(b -> {
                            b.setAttribute(ATTS_MAPPING.get(it.getAttribute()));
                            return Unit.INSTANCE;
                        });
                    } else {
                        return it;
                    }
                }).collect(Collectors.toList());

                RecordsQuery registryQuery = recordsQuery.copy()
                    .withSourceId(FormsRegistryConfiguration.FORMS_REGISTRY_SOURCE_ID)
                    .withQuery(predicate)
                    .withSortBy(mappedSortBy)
                    .build();

                RecsQueryRes<RecordRef> queryRes = recordsService.query(registryQuery);

                List<EntityWithMeta<EcosFormDef>> journals = ecosFormService.getFormsByIds(queryRes.getRecords()
                    .stream()
                    .map(RecordRef::getId)
                    .collect(Collectors.toList())
                );
                result.setRecords(journals.stream()
                    .map(EntityWithMeta::getEntity)
                    .map(this::toRecord)
                    .collect(Collectors.toList()));
                result.setTotalCount(queryRes.getTotalCount());

            } else {
                List<EcosFormRecord> forms = ecosFormService.getAllForms(
                        VoidPredicate.INSTANCE,
                        max,
                        skipCount,
                        recordsQuery.getSortBy()
                    )
                    .stream()
                    .map(this::toRecord)
                    .collect(Collectors.toList());

                result.setRecords(forms);
                result.setTotalCount(ecosFormService.getCount(VoidPredicate.INSTANCE));
            }
            return result;
        }

        Optional<EcosFormDef> form = Optional.empty();

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

    @Autowired(required = false)
    public void setRecordEventsService(RecordEventsService recordEventsService) {
        this.recordEventsService = recordEventsService;
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
