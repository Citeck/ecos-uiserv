package ru.citeck.ecos.uiserv.dto.journal;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;

@Data
public class JournalColumnDto {

    private String attribute;

    private RecordRef editorRef;

    private String type;

    private boolean searchable;

    private boolean sortable;

    private boolean groupable;

    private boolean editable;

    private MLText name;

    private JournalConfigDto formatter;

    private boolean show;

    private boolean visible;

    private JournalConfigDto options;

    private ObjectData attributes;

    private JournalConfigDto filter;

}
