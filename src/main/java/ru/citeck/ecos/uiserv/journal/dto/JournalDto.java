package ru.citeck.ecos.uiserv.journal.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;

import java.util.ArrayList;
import java.util.List;

@Data
public class JournalDto {

    /**
     * Journal identifier.
     */
    private String id;

    /**
     * Journal name. Field for information.
     */
    private MLText name;

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
    private boolean editable;

    /**
     * Journal columns to display in table.
     */
    private List<JournalColumnDto> columns;

    /**
     * Custom attributes for temporal or very specific
     * parameters which can't be added as field for this DTO
     */
    private ObjectData attributes;
}
