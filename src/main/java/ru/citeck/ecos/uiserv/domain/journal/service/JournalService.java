package ru.citeck.ecos.uiserv.domain.journal.service;

import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public interface JournalService {

    long getLastModifiedTimeMs();

    JournalWithMeta getJournalById(String id);

    List<JournalWithMeta> getAll(int max, int skipCount, Predicate predicate);

    JournalWithMeta getById(String id);

    Set<JournalWithMeta> getAll(int maxItems, int skipCount);

    Set<JournalWithMeta> getAll(Set<String> extIds);

    long getCount();

    long getCount(Predicate predicate);

    void onJournalChanged(BiConsumer<JournalDef, JournalDef> consumer);

    JournalWithMeta save(JournalDef dto);

    void delete(String id);
}
