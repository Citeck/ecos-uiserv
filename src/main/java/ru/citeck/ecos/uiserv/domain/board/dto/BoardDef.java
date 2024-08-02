package ru.citeck.ecos.uiserv.domain.board.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;

@Data
public class BoardDef {

    private String id;

    private EntityRef typeRef;
    private EntityRef journalRef;
    private EntityRef cardFormRef;

    private Boolean readOnly;
    private Boolean disableTitle;

    private MLText name;
    private List<EntityRef> actions;
    private List<BoardColumnDef> columns;

    public BoardDef() {
    }

    public BoardDef(String id) {
        this.id = id;
    }

    public BoardDef(BoardDef other) {
        this.id = other.getId();
        this.typeRef = other.getTypeRef();
        this.journalRef = other.getJournalRef();
        this.cardFormRef = other.getCardFormRef();
        this.readOnly = other.getReadOnly();
        this.disableTitle = other.getDisableTitle();
        this.name = other.getName();
        this.actions = other.getActions();
        this.columns = other.getColumns();
    }

    @JsonIgnore
    public EntityRef getRef() {
        return EntityRef.create(Application.NAME, BoardRecordsDao.ID, id);
    }

    public static EntityRef createRef(String localId) {
        return EntityRef.create(Application.NAME, BoardRecordsDao.ID, localId);
    }

    public EntityRef getTypeRef() {
        return typeRef;
    }
}
