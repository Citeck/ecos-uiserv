package ru.citeck.ecos.uiserv.domain.journal.service;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.domain.form.repo.EcosFormEntity;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface JournalService {

    long getLastModifiedTimeMs();

    void updateJournalType(String formId, RecordRef typeRef);

    JournalWithMeta getJournalById(String id);

    String getJournalsListIdByJournalId(String journalId);

    List<JournalWithMeta> getAll(int max, int skipCount, Predicate predicate);

    JournalWithMeta getById(String id);

    Set<JournalWithMeta> getAll(int maxItems, int skipCount);

    Set<JournalWithMeta> getAll(Set<String> extIds);

    long getCount();

    long getCount(Predicate predicate);

    void onJournalChanged(Consumer<JournalDto> consumer);

    JournalWithMeta save(JournalDto dto);

    void delete(String id);

    List<JournalWithMeta> getJournalsByJournalList(String journalListId);

    List<JournalWithMeta> getJournalsWithSite();
}
