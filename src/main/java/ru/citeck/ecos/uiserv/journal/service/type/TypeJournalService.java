package ru.citeck.ecos.uiserv.journal.service.type;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.service.JournalService;
import ru.citeck.ecos.uiserv.service.form.EcosFormModel;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypeJournalService {

    public static final String JOURNAL_ID_PREFIX = "type_";

    private final EcosFormService formService;
    private final RecordsService recordsService;
    private final JournalService journalService;
    private final JournalByFormGenerator byFormGenerator;

    private LoadingCache<RecordRef, Optional<JournalDto>> journalByTypeCache;
    private LoadingCache<String, Optional<JournalDto>> journalByFormIdCache;

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

    public Optional<JournalDto> getJournalForType(RecordRef typeRef) {
        if (RecordRef.isEmpty(typeRef)) {
            return Optional.empty();
        }
        return journalByTypeCache.getUnchecked(typeRef);
    }

    public List<JournalDto> getJournalsByListId(String journalListId) {

        Set<String> journalsId = new HashSet<>();
        List<JournalDto> result = new ArrayList<>();

        getJournalsFromTypesByListId(journalListId)
            .stream()
            .filter(j -> journalsId.add(j.getId()))
            .forEach(result::add);

        return result;
    }

    private List<JournalDto> getJournalsFromTypesByListId(String journalListId) {

        RecordsQuery query = new RecordsQuery();
        query.setLanguage("journal-list");
        query.setSourceId("emodel/type");
        query.setQuery(new ObjectData("{\"listId\":\"" + journalListId + "\"}"));

        try {
            RecordsQueryResult<RecordRef> result = recordsService.queryRecords(query);
            return result.getRecords()
                .stream()
                .map(this::getJournalForType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Types can't be received from emodel. List id: '" + journalListId + "'", e);
            return Collections.emptyList();
        }
    }

    private Optional<JournalDto> getJournalForTypeImpl(RecordRef typeRef) {

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

        Optional<JournalDto> journal = getJournal(
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

    private Optional<JournalDto> getJournalForTypeImpl(List<RecordRef> types, int idx) {

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

        Optional<JournalDto> journal = getJournal(
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

    private Optional<JournalDto> getJournal(RecordRef typeRef,
                                            String sourceId,
                                            RecordRef journalRef,
                                            RecordRef formRef) {

        if (RecordRef.isNotEmpty(journalRef)) {
            JournalDto journal = journalService.getJournalById(journalRef.getId());
            if (journal != null) {
                return Optional.of(journal);
            }
        }
        if (RecordRef.isNotEmpty(formRef)) {
            return journalByFormIdCache.getUnchecked(formRef.getId())
                .map(dto -> {
                    JournalDto result = new JournalDto(dto);
                    result.setTypeRef(typeRef);
                    result.setSourceId(sourceId);
                    result.setPredicate(new ObjectData(Predicates.eq("_etype", typeRef.toString())));
                    result.setId(JOURNAL_ID_PREFIX + typeRef.getId());
                    return result;
                });
        }
        return Optional.empty();
    }

    private Optional<JournalDto> getJournalByFormIdImpl(String formId) {

        EcosFormModel form = formService.getFormById(formId).orElse(null);
        if (form == null) {
            return Optional.empty();
        }

        JournalDto journal = new JournalDto();
        byFormGenerator.fillData(journal, form);

        if (journal.getColumns().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(journal);
    }

    @Data
    public static class TypeMetaWithParents {

        private String sourceId;
        private RecordRef journal;
        private RecordRef form;

        @MetaAtt("parents[]?id")
        private List<RecordRef> parentsRefs;
    }

    @Data
    public static class TypeMeta {

        private String sourceId;
        private RecordRef journal;
        private RecordRef form;
    }
}
