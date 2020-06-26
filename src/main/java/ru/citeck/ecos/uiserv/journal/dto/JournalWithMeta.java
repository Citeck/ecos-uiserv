package ru.citeck.ecos.uiserv.journal.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
public class JournalWithMeta extends JournalDto {

    private ObjectData fullPredicate;

    @MetaAtt("_modified")
    private Instant modified;
    @MetaAtt("_modifier")
    private String modifier;

    @MetaAtt("_creator")
    private Instant created;
    @MetaAtt("_creator")
    private String creator;

    public JournalWithMeta() {
    }

    public JournalWithMeta(JournalWithMeta other) {
        super(other);
        this.fullPredicate = ObjectData.deepCopy(other.fullPredicate);
        this.modified = other.modified;
        this.modifier = other.modifier;
        this.created = other.created;
        this.creator = other.creator;
    }
}
