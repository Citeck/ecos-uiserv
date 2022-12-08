package ru.citeck.ecos.uiserv.domain.journal.service;

import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.domain.journal.service.provider.JournalsProvider;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface JournalService {

    long getLastModifiedTimeMs();

    JournalWithMeta getJournalById(String id);

    List<JournalWithMeta> getAll(Predicate predicate, int max, int skip, List<SortBy> sort);

    Set<JournalWithMeta> getAll(int maxItems, int skipCount);

    List<JournalWithMeta> getAll(Collection<String> extIds);

    long getCount();

    long getCount(Predicate predicate);

    void onJournalDeleted(Consumer<JournalWithMeta> consumer);

    void onJournalChanged(BiConsumer<JournalDef, JournalDef> consumer);

    void onJournalWithMetaChanged(BiConsumer<JournalWithMeta, JournalWithMeta> consumer);

    JournalWithMeta save(JournalDef dto);

    void delete(String id);

    void registerProvider(JournalsProvider provider);
}
