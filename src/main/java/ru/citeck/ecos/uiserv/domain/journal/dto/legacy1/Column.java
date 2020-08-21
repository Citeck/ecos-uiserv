package ru.citeck.ecos.uiserv.domain.journal.dto.legacy1;

import lombok.Data;
import ru.citeck.ecos.uiserv.domain.journal.dto.ColumnFormatter;

import java.util.Map;

@Data
public class Column {

    private String text;
    private String type;
    private String editorKey;
    private String javaClass;
    private String attribute;
    private String schema;
    private String innerSchema;
    // control from new config
    private Formatter formatter;
    // new formatters
    private ColumnFormatter newFormatter;
    private Map<String, String> params;
    private boolean isDefault;
    private boolean isSearchable;
    private boolean isSortable;
    private boolean isVisible;
    private boolean hidden;
    private boolean isGroupable;
    private boolean batchEdit;
    private Boolean multiple;
}
