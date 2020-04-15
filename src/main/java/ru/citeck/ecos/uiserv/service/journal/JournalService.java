package ru.citeck.ecos.uiserv.service.journal;

import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.dto.journal.JournalDto;
import ru.citeck.ecos.uiserv.service.action.ActionModule;
import ru.citeck.ecos.uiserv.web.records.record.JournalRecord;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

public interface JournalService {

    Set<JournalDto> getAll(int max, int skipCount, Predicate predicate);

    JournalDto getById(String id);

    Set<JournalDto> getAll(int maxItems, int skipCount);

    Set<JournalDto> getAll(Set<String> extIds);

    @Nullable
    JournalDto searchJournalByTypeRef(RecordRef typeRef);

    long getCount();

    long getCount(Predicate predicate);

    void onJournalChanged(Consumer<JournalDto> consumer);

    JournalDto update(JournalDto dto);
}
