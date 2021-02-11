package ru.citeck.ecos.uiserv.domain.journal.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
public class JournalWithMeta {

    @AttName("...")
    private JournalDef journalDef;

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
        this.journalDef = other.journalDef;
        this.modified = other.modified;
        this.modifier = other.modifier;
        this.created = other.created;
        this.creator = other.creator;
    }

    public JournalDef getJournalDef() {
        return journalDef;
    }

    public void setJournalDef(JournalDef journalDef) {
        this.journalDef = journalDef;
    }

    public Instant getModified() {
        return modified;
    }

    public void setModified(Instant modified) {
        this.modified = modified;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}
