package ru.citeck.ecos.uiserv.domain.board.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;

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

    public String getLocalId() {
        if (boardDef != null)
            return boardDef.getId();
        return "";
    }

    public BoardWithMeta() {
    }

    public BoardWithMeta(String id) {
        boardDef = new BoardDef(id);
    }

    public BoardWithMeta(BoardWithMeta other) {
        this.boardDef = other.boardDef;
        this.modified = other.modified;
        this.modifier = other.modifier;
        this.created = other.created;
        this.creator = other.creator;
    }

    public RecordRef getRef() {
        return RecordRef.create(BoardEntity.APP_NAME, BoardEntity.SOURCE_ID, getLocalId());
    }
}
