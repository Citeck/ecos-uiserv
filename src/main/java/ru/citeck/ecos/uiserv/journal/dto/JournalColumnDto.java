package ru.citeck.ecos.uiserv.journal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JournalColumnDto {

    /**
     * Internal column name. Used in data query when attribute is not set.
     * Allowed characters: [a-zA-Z\-_:]+.
     *
     * Mandatory
     */
    private String name;

    /**
     * Label to display in column header.
     */
    private MLText label;

    /**
     * Attribute to load data.
     * Can be complex, e.g. ecos:counterparty.ecos:inn?str
     *
     * Optional. If not specified field 'name' should be used to load data
     *
     * @see ru.citeck.ecos.records2.RecordsService#getAttribute(RecordRef, String)
     */
    private String attribute;

    /**
     * Configuration for inline or group attribute editor.
     */
    private ColumnEditorDto editor;

    /**
     * Configuration for filter.
     */
    private ColumnFilterDto filter;

    /**
     * Configuration for formatter.
     * Required to change default display logic in table.
     */
    private ColumnFormatterDto formatter;

    /**
     * Attribute options. Can be used in filter and editor.
     */
    private JournalOptionsDto options;

    /**
     * Data type.
     * ["text", "int", "long", "float", "options", "assoc", etc. ]
     */
    private String type;

    /**
     * Is filtering allowed for this column?
     */
    private boolean searchable;

    /**
     * Is sorting allowed for this column?
     */
    private boolean sortable;

    /**
     * Can data be grouped by this column?
     */
    private boolean groupable;

    /**
     * Is column editable?
     */
    private boolean editable;

    /**
     * Is column visible or not.
     * This parameter can be changed by user in UI
     */
    private boolean visible;

    /**
     * Hidden column won't be displayed in UI.
     * This parameter required when some extra data is required to load.
     */
    private boolean hidden;

    /**
     * Custom attributes for temporal or very specific
     * parameters which can't be added as field for this DTO
     */
    private ObjectData attributes;
}
