package ru.citeck.ecos.uiserv.domain.board.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;

@Data
public class BoardDef {
    private String id;

    //private String extId;
    private RecordRef typeRef;
    private RecordRef journalRef;
    private RecordRef cardFormRef;

    private Boolean readOnly;

    private MLText name;
    private List<RecordRef> actions;
    private List<BoardColumnDef> columns;

    public BoardDef(){
    }
    public BoardDef(String id){
        this.id = id;
    }
    public BoardDef(BoardDef other){
        this.id = other.getId();
        this.typeRef = other.getTypeRef();
        this.journalRef = other.getJournalRef();
        this.cardFormRef = other.getCardFormRef();
        this.readOnly = other.getReadOnly();
        this.name = other.getName();
        this.actions = other.getActions();
        this.columns = other.getColumns();
    }
}
