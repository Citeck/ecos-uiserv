package ru.citeck.ecos.uiserv.domain.journal.dto.legacy0;

import lombok.Data;
import ru.citeck.ecos.commons.data.DataValue;

import java.util.List;
import java.util.Map;

@Data
public class JournalTypeDto {

    private String id;
    private String datasource;
    private Map<String, String> settings;
    private DataValue groupActions;
    private List<Attribute> attributes;
}

