package ru.citeck.ecos.uiserv.domain.journal.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.JsonMapper;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.uiserv.domain.journal.dto.legacy1.GroupAction;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
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
    private ObjectData predicate = ObjectData.create();

    /**
     * Group records by specified attributes.
     */
    private List<String> groupBy = new ArrayList<>();

    /**
     * Default sorting.
     */
    private List<JournalSortBy> sortBy = new ArrayList<>();

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
    private ObjectData attributes = ObjectData.create();

    /**
     * This actions in future will be moved to 'actions' field
     */
    @Deprecated
    private List<GroupAction> groupActions = new ArrayList<>();

    private List<CreateVariantDto> createVariants = new ArrayList<>();

    public JournalDto() {
    }

    public JournalDto(JournalDto other) {

        JsonMapper mapper = Json.getMapper();

        this.id = other.id;
        this.label = mapper.copy(other.label);
        this.sourceId = other.sourceId;
        this.metaRecord = other.metaRecord;
        this.typeRef = other.typeRef;
        this.predicate = mapper.copy(other.predicate);
        this.groupBy = mapper.copy(other.groupBy);
        this.sortBy = mapper.copy(other.sortBy);
        this.actions = mapper.copy(other.actions);
        this.editable = other.editable;
        this.columns = DataValue.create(other.columns).asList(JournalColumnDto.class);
        this.attributes = mapper.copy(other.attributes);
        this.groupActions = DataValue.create(other.groupActions).asList(GroupAction.class);
        this.createVariants = DataValue.create(other.createVariants).asList(CreateVariantDto.class);
    }
}
