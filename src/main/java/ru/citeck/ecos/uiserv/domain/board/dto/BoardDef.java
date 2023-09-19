package ru.citeck.ecos.uiserv.domain.board.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao;

import java.util.List;

@Data
public class BoardDef {

    private String id;

    private RecordRef typeRef;
    private RecordRef journalRef;
    private RecordRef cardFormRef;

    private Boolean readOnly;
    private Boolean disableTitle;

    private MLText name;
    private List<RecordRef> actions;
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
    public RecordRef getRef() {
        return RecordRef.create(Application.NAME, BoardRecordsDao.ID, id);
    }

    public static RecordRef createRef(String localId) {
        return RecordRef.create(Application.NAME, BoardRecordsDao.ID, localId);
    }

    public RecordRef getTypeRef() {
        return typeRef;
    }
}
