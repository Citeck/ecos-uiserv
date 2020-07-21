package ru.citeck.ecos.uiserv.domain.journal.api.records.format;

import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;

public interface JournalModelFormat<T> {

    T convert(JournalWithMeta dto);

    int getVersion();
}
