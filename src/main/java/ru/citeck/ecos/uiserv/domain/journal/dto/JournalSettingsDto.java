package ru.citeck.ecos.uiserv.domain.journal.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class JournalSettingsDto {

    private String id;
    private String name;
    private String authority;
    private String journalId;

    private ObjectData settings;
}
