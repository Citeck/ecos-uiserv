package ru.citeck.ecos.uiserv.journal.dto.legacy1;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class JournalConfigResp {
    private String id;
    private String sourceId;
    private JournalMeta meta;
    private List<Column> columns;
    private Map<String, String> params;
}
