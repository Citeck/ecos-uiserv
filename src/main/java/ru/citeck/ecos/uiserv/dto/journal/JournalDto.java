package ru.citeck.ecos.uiserv.dto.journal;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.Predicate;

import java.util.HashSet;
import java.util.Set;

@Data
public class JournalDto {

    private String id;

    private MLText name;

    private String metaRecord;

    private RecordRef typeRef;

    private JsonNode predicate;

    private Set<RecordRef> actions = new HashSet<>();

    private boolean editable;

    private Set<JournalColumnDto> columns = new HashSet<>();

    private ObjectData attributes;
}
