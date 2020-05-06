package ru.citeck.ecos.uiserv.journal.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
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
    private RecordRef metaRecord = RecordRef.EMPTY;

    /**
     * ECOS type.
     */
    private RecordRef typeRef = RecordRef.EMPTY;

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
    private List<RecordRef> actions;

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
        JournalDto copy = Json.getMapper().copy(other);
        if (copy == null) {
            return;
        }
        this.id = copy.id;
        this.label = copy.label;
        this.sourceId = copy.sourceId;
        this.metaRecord = copy.metaRecord;
        this.typeRef = copy.typeRef;
        this.predicate = copy.predicate;
        this.groupBy = copy.groupBy;
        this.sortBy = copy.sortBy;
        this.actions = copy.actions;
        this.editable = copy.editable;
        this.columns = copy.columns;
        this.attributes = copy.attributes;
    }
}
