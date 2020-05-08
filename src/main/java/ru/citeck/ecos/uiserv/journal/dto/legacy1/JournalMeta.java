package ru.citeck.ecos.uiserv.journal.dto.legacy1;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;

@Data
public class JournalMeta {
    private String nodeRef;
    private List<Criterion> criteria;
    private String title;
    private JsonNode predicate;
    private JsonNode groupBy;
    private String metaRecord;
    private List<CreateVariant> createVariants;
    private List<RecordRef> actions;
    private List<GroupAction> groupActions;
}
