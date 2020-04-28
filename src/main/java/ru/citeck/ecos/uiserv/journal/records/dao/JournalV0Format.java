package ru.citeck.ecos.uiserv.journal.records.dao;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.JournalTypeDto;

@Component
public class JournalV0Format implements JournalModelFormat<JournalTypeDto> {

    @Override
    public JournalTypeDto convert(JournalDto dto) {
        //todo
        return null;
    }

    @Override
    public int getVersion() {
        return 0;
    }
}
