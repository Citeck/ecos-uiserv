package ru.citeck.ecos.uiserv.journal.records.record;

import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.uiserv.config.ApplicationConfig;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class JournalRecord implements MetaValue {

    public final static String JOURNAL_DAO_RECORD_PREFIX = "journal@";
    public final static String FULL_RECORD_PREFIX = ApplicationConfig.APP_NAME + "/" + JOURNAL_DAO_RECORD_PREFIX;
    private final static ObjectMapper objectMapper = new ObjectMapper();

    private String id;

    private MLText name;

    private RecordRef metaRecord;

    private RecordRef typeRef;

    private ObjectData predicate;

    private Set<String> actions = new HashSet<>();

    private boolean editable;

    private String columns;

    private ObjectData attributes;

    public JournalRecord(JournalDto dto) {
        this.id = dto.getId();
        this.name = dto.getName();
        this.metaRecord = dto.getMetaRecord();
        this.typeRef = dto.getTypeRef();
        this.predicate = dto.getPredicate();

        if (dto.getActions() != null) {
            this.actions = dto.getActions().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
        }
        this.editable = dto.isEditable();
        this.columns = dto.getColumns();
        this.attributes = dto.getAttributes();
    }

    @Override
    public String getId() {
        return FULL_RECORD_PREFIX + id;
    }

    @Override
    public String getDisplayName() {
        if (name == null) {
            return id;
        } else {
            return name.getClosestValue(Locale.ENGLISH);
        }
    }

    @Override
    public Object getAttribute(String name, MetaField field) {
        switch (name) {
            case "id":
                return this.id;
            case "name":
                return this.name;
            case "metaRecord":
                return metaRecord;
            case "typeRef":
                return typeRef;
            case "predicate":
                return predicate;
            case "actions":
                return actions;
            case "editable":
                return editable;
            case "columns":
                return columns;
            case "attributes":
                return attributes;
        }
        return null;
    }
}
