package ru.citeck.ecos.uiserv.journal.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;

import java.util.ArrayList;
import java.util.List;

@Data
public class JournalDto {

    private String id;

    private MLText name;

    private RecordRef metaRecord;

    private RecordRef typeRef;

    private ObjectData predicate;

    private List<RecordRef> actions = new ArrayList<>();

    private boolean editable;

    private String columns;

    private ObjectData attributes;
}
