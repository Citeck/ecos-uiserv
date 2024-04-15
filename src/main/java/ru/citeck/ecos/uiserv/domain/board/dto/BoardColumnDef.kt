package ru.citeck.ecos.uiserv.domain.board.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault;

@Data
@IncludeNonDefault
public class BoardColumnDef {
    /**
     * Internal column name corresponds to a <i>board type</i> status ID.
     *
     * Mandatory
     */
    private String id;
    /**
     * Name to display in column header.
     */
    private MLText name;

    public BoardColumnDef(){
    }

    public BoardColumnDef(BoardColumnDef base){
        this.id = base.id;
        this.name = base.name;
    }

    public BoardColumnDef(String id, MLText name) {
        this.id = id;
        this.name = name;
    }
}
