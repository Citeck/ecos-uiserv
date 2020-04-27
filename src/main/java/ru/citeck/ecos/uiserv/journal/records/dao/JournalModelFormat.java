package ru.citeck.ecos.uiserv.journal.records.dao;

import ru.citeck.ecos.uiserv.journal.dto.JournalDto;

public interface JournalModelFormat<T> {

    T convert(JournalDto dto);

    int getVersion();
}
