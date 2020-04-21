package ru.citeck.ecos.uiserv.journal.eapps.module;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OldJournalModule {

    private String id;

    private String name;

    private RecordRef metaRecord;

    private ObjectData predicate;

    private List<RecordRef> actions;

    private String columnsJSONStr;

    private Map<String, String> attributes;
}
