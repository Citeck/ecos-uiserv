package ru.citeck.ecos.uiserv.domain.journal.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;

@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
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
     * Optional. If not specified field 'name' will be used to load data
     *
     * @see ru.citeck.ecos.records2.RecordsService#getAttribute(RecordRef, String)
     */
    private String attribute;

    /**
     * Configuration for control.
     * Control should define formatter, editor and filter rules.
     */
    private ColumnControl control;

    /**
     * Data type.
     * ["text", "int", "long", "float", "options", "assoc", etc. ]
     */
    private String type;

    /**
     * Is filtering allowed for this column?
     */
    private Boolean searchable;

    /**
     * Is sorting allowed for this column?
     */
    private Boolean sortable;

    /**
     * Can data be grouped by this column?
     */
    private Boolean groupable;

    /**
     * Is column editable?
     */
    private Boolean editable;

    /**
     * Is column visible or not.
     * This parameter can be changed by user in UI
     */
    private Boolean visible;

    /**
     * Hidden column won't be displayed in UI.
     * This parameter required when some extra data is required to load.
     */
    private Boolean hidden;

    /**
     * Custom attributes for temporal or very specific
     * parameters which can't be added as field for this DTO
     */
    private ObjectData attributes;

    public JournalColumnDto(JournalColumnDto other) {
        name = other.name;
        label = MLText.copy(other.label);
        attribute = other.attribute;
        control = Json.getMapper().copy(other.control);
        type = other.type;
        searchable = other.searchable;
        sortable = other.sortable;
        groupable = other.groupable;
        editable = other.editable;
        visible = other.visible;
        hidden = other.hidden;
        attributes = ObjectData.deepCopy(other.attributes);
    }
}
