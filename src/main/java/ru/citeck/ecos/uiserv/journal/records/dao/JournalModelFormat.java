package ru.citeck.ecos.uiserv.journal.records.dao;

import ru.citeck.ecos.uiserv.journal.dto.JournalWithMeta;

public interface JournalModelFormat<T> {

    T convert(JournalWithMeta dto);

    int getVersion();
}
