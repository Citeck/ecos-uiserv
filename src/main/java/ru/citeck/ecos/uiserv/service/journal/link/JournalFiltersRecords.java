package ru.citeck.ecos.uiserv.service.journal.link;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.uiserv.domain.JournalFilter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JournalFiltersRecords extends LocalRecordsDAO
    implements MutableRecordsLocalDAO<JournalFilter>, LocalRecordsMetaDAO<JournalFilter> {

    public static final String ID = "user-conf";

    private final JournalFiltersService journalFiltersService;

    public JournalFiltersRecords(JournalFiltersService journalFiltersService) {
        setId(ID);
        this.journalFiltersService = journalFiltersService;
    }

    @Override
    public RecordsMutResult save(List<JournalFilter> values) {
        RecordsMutResult result = new RecordsMutResult();
        for (JournalFilter value : values) {
            JournalFilter saved = journalFiltersService.save(value);
            result.addRecord(new RecordMeta(saved.getExternalId()));
        }
        return result;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        RecordsDelResult result = new RecordsDelResult();
        for (RecordRef record : deletion.getRecords()) {
            journalFiltersService.deleteByExternalId(record.getId());
            result.addRecord(new RecordMeta(record));
        }
        return result;
    }

    @Override
    public List<JournalFilter> getValuesToMutate(List<RecordRef> records) {
        return getRecordsFromRecordRefs(records);
    }

    @Override
    public List<JournalFilter> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return getRecordsFromRecordRefs(records);
    }

    private List<JournalFilter> getRecordsFromRecordRefs(Collection<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(extId ->
                Optional.ofNullable(journalFiltersService.findByExternalId(extId))
                    .orElseGet(JournalFilter::new)
            ).collect(Collectors.toList());
    }
}
