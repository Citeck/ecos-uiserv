package ru.citeck.ecos.uiserv.domain.board.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
public class BoardWithMeta {
    @AttName("...")
    private BoardDef boardDef;

    @AttName("_modified")
    private Instant modified;
    @AttName("_modifier")
    private String modifier;

    @AttName("_creator")
    private Instant created;
    @AttName("_creator")
    private String creator;

    public BoardWithMeta() {
    }
    public BoardWithMeta(BoardWithMeta other) {
        this.boardDef = other.boardDef;
        this.modified = other.modified;
        this.modifier = other.modifier;
        this.created = other.created;
        this.creator = other.creator;
    }
}
