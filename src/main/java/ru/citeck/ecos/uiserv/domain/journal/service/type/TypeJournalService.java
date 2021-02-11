package ru.citeck.ecos.uiserv.domain.journal.service.type;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypeJournalService {

    public static final String JOURNAL_ID_PREFIX = "type_";

    private final EcosFormService formService;
    private final RecordsService recordsService;
    private final JournalService journalService;
    private final JournalByFormGenerator byFormGenerator;

    private LoadingCache<RecordRef, Optional<JournalWithMeta>> journalByTypeCache;
    private LoadingCache<String, Optional<JournalWithMeta>> journalByFormIdCache;

    @PostConstruct
    public void init() {

        journalByTypeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(100)
            .build(CacheLoader.from(this::getJournalForTypeImpl));

        journalByFormIdCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .maximumSize(100)
            .build(CacheLoader.from(this::getJournalByFormIdImpl));

        formService.addChangeListener(form -> journalByFormIdCache.invalidate(form.getId()));
    }

    public Optional<JournalWithMeta> getJournalForType(RecordRef typeRef) {
        if (RecordRef.isEmpty(typeRef)) {
            return Optional.empty();
        }
        return journalByTypeCache.getUnchecked(typeRef);
    }

    private Optional<JournalWithMeta> getJournalForTypeImpl(RecordRef typeRef) {

        if (RecordRef.isEmpty(typeRef)) {
            return Optional.empty();
        }

        TypeMetaWithParents typeMeta = null;
        try {
            typeMeta = recordsService.getMeta(typeRef, TypeMetaWithParents.class);
        } catch (Exception e) {
            log.error("Type meta can't be received for type: " + typeRef, e);
        }
        if (typeMeta == null) {
            return Optional.empty();
        }

        Optional<JournalWithMeta> journal = getJournal(
            typeRef,
            typeMeta.getSourceId(),
            typeMeta.getJournal(),
            typeMeta.getForm()
        );
        if (!journal.isPresent()) {
            journal = getJournalForTypeImpl(typeMeta.parentsRefs, 0);
        }
        return journal;
    }

    private Optional<JournalWithMeta> getJournalForTypeImpl(List<RecordRef> types, int idx) {

        if (types == null || types.isEmpty() || idx >= types.size()) {
            return Optional.empty();
        }

        RecordRef typeRef = types.get(idx);

        TypeMeta meta = null;
        try {
            meta = recordsService.getMeta(typeRef, TypeMeta.class);
        } catch (Exception e) {
            log.error("Type meta can't be received for type: " + typeRef, e);
        }
        if (meta == null) {
            return Optional.empty();
        }

        Optional<JournalWithMeta> journal = getJournal(
            typeRef,
            meta.getSourceId(),
            meta.getJournal(),
            meta.getForm()
        );
        if (!journal.isPresent()) {
            journal = getJournalForTypeImpl(types, idx + 1);
        }
        return journal;
    }

    private Optional<JournalWithMeta> getJournal(RecordRef typeRef,
                                                 String sourceId,
                                                 RecordRef journalRef,
                                                 RecordRef formRef) {

        if (RecordRef.isNotEmpty(journalRef)) {
            JournalWithMeta journal = journalService.getJournalById(journalRef.getId());
            if (journal != null) {
                return Optional.of(journal);
            }
        }
        if (RecordRef.isNotEmpty(formRef)) {
            return journalByFormIdCache.getUnchecked(formRef.getId())
                .map(dto -> {
                    JournalWithMeta result = new JournalWithMeta(dto);
                    result.setJournalDef(JournalDef.create()
                        .withTypeRef(typeRef)
                        .withPredicate(Predicates.eq("_type", typeRef.toString()))
                        .withId(JOURNAL_ID_PREFIX + typeRef.getId())
                        .build());
                    return result;
                });
        }
        return Optional.empty();
    }

    private Optional<JournalWithMeta> getJournalByFormIdImpl(String formId) {

        return Optional.empty();
        /*EcosFormModel form = formService.getFormById(formId).orElse(null);
        if (form == null) {
            return Optional.empty();
        }

        JournalWithMeta journal = new JournalWithMeta();
        byFormGenerator.fillData(journal, form);

        if (journal.getJournalDef().getColumns().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(journal);*/
    }

    @Data
    public static class TypeMetaWithParents {

        private String sourceId;
        private RecordRef journal;
        private RecordRef form;

        @AttName("parents[]?id")
        private List<RecordRef> parentsRefs;
    }

    @Data
    public static class TypeMeta {

        private String sourceId;
        private RecordRef journal;
        private RecordRef form;
    }
}
