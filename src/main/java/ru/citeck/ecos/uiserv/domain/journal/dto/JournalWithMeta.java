package ru.citeck.ecos.uiserv.domain.journal.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
public class JournalWithMeta extends JournalDto {

    @AttName("_modified")
    private Instant modified;
    @AttName("_modifier")
    private String modifier;

    @AttName("_creator")
    private Instant created;
    @AttName("_creator")
    private String creator;

    public JournalWithMeta() {
    }

    public JournalWithMeta(JournalWithMeta other) {
        super(other);
        this.modified = other.modified;
        this.modifier = other.modifier;
        this.created = other.created;
        this.creator = other.creator;
    }
}
