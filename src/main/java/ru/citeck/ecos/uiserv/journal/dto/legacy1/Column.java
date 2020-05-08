package ru.citeck.ecos.uiserv.journal.dto.legacy1;

import lombok.Data;

import java.util.Map;

@Data
public class Column {

    private String text;
    private String type;
    private String editorKey;
    private String javaClass;
    private String attribute;
    private String schema;
    private Formatter formatter;
    private Map<String, String> params;
    private boolean isDefault;
    private boolean isSearchable;
    private boolean isSortable;
    private boolean isVisible;
    private boolean isGroupable;
}
