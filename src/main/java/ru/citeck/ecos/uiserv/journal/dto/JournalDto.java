package ru.citeck.ecos.uiserv.journal.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class JournalDto {

    /**
     * Journal identifier.
     */
    private String id;

    /**
     * Journal label. Field for information.
     */
    private MLText label;

    /**
     * Records source ID.
     */
    private String sourceId;

    /**
     * Record to load metadata from edge.
     *
     * @see ru.citeck.ecos.records2.graphql.meta.value.MetaValue#getEdge(String, MetaField)
     * @see ru.citeck.ecos.records2.graphql.meta.value.MetaEdge
     */
    private RecordRef metaRecord;

    /**
     * ECOS type.
     */
    private RecordRef typeRef;

    /**
     * Predicate for base entities filtering in a table.
     * This predicate can't be changed by user
     * and always should be joined by other filter predicates by AND
     */
    private ObjectData predicate;

    /**
     * Group records by specified attributes.
     */
    private List<String> groupBy;

    /**
     * Default sorting.
     */
    private List<JournalSortBy> sortBy;

    /**
     * Actions for every entity in a table.
     * Can be filtered for specific entities by evaluator.
     */
    private List<RecordRef> actions = new ArrayList<>();

    /**
     * Can attributes of entities in a table be edited or not.
     * Global config for all columns.
     * If manual setup for columns is required see JournalColumnDto::editable.
     */
    private Boolean editable;

    /**
     * Journal columns to display in table.
     */
    @NotNull
    private List<JournalColumnDto> columns = new ArrayList<>();

    /**
     * Custom attributes for temporal or very specific
     * parameters which can't be added as field for this DTO
     */
    private ObjectData attributes;

    public JournalDto() {
    }

    public JournalDto(JournalDto other) {
        this.id = other.id;
        this.label = Json.getMapper().copy(other.label);
        this.sourceId = other.sourceId;
        this.metaRecord = other.metaRecord;
        this.typeRef = other.typeRef;
        this.predicate = Json.getMapper().copy(other.predicate);
        this.groupBy = Json.getMapper().copy(other.groupBy);
        this.sortBy = other.sortBy;
        this.actions = Json.getMapper().copy(other.actions);
        this.editable = other.editable;
        List<JournalColumnDto> columns = Json.getMapper().copy(other.columns);
        this.columns = columns != null ? columns : new ArrayList<>();
        this.attributes = Json.getMapper().copy(other.attributes);
    }
}
