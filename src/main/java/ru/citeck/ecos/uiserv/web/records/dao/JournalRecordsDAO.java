package ru.citeck.ecos.uiserv.web.records.dao;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.Elements;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records2.predicate.RecordElements;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.uiserv.dto.journal.JournalDto;
import ru.citeck.ecos.uiserv.service.journal.JournalService;
import ru.citeck.ecos.uiserv.web.records.record.JournalRecord;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JournalRecordsDAO extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<JournalRecord>, LocalRecordsMetaDAO<JournalRecord> {

    private static final JournalRecord EMPTY_RECORD = new JournalRecord(new JournalDto());

    private final JournalService journalService;

    @Override
    public RecordsQueryResult<JournalRecord> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {
        RecordsQueryResult<JournalRecord> result = new RecordsQueryResult<>();

        Map<String, String> query = (Map<String, String>)recordsQuery.getQuery();
        if (query != null && query.containsKey("typeRef")) {

            RecordRef typeRef = RecordRef.valueOf(query.get("typeRef"));
            JournalDto dto = journalService.searchJournalByTypeRef(typeRef);
            if (dto != null) {
                JournalRecord journalRecord = new JournalRecord(dto);
                result.addRecord(journalRecord);
            }
            return result;
        }

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {
            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            int max = recordsQuery.getMaxItems();
            if (max <= 0) {
                max = 10000;
            }

            Set<JournalDto> journals = journalService.getAll(max, recordsQuery.getSkipCount(), predicate);

            result.setRecords(journals.stream()
                .map(JournalRecord::new)
                .collect(Collectors.toList()));
            result.setTotalCount(journalService.getCount(predicate));

        } else {
            result.setRecords(journalService.getAll(recordsQuery.getMaxItems(), recordsQuery.getSkipCount()).stream()
                .map(JournalRecord::new)
                .collect(Collectors.toList()));
            result.setTotalCount(journalService.getCount());
        }
        return result;
    }

    @Override
    public List<JournalRecord> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {
        if (list.size() == 1 && list.get(0).getId().isEmpty()) {
            return Collections.singletonList(EMPTY_RECORD);
        }

        return list.stream()
            .map(ref -> new JournalRecord(journalService.getById(ref.getId())))
            .collect(Collectors.toList());
    }

    @Data
    public static class QueryWithTypeRef {
        private String typeRef;
    }

    @Override
    public String getId() {
        return "journal";
    }
}
