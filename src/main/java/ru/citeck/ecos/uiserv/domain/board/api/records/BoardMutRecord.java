package ru.citeck.ecos.uiserv.domain.board.api.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;

import java.util.List;

public class BoardMutRecord extends BoardDef {

    public BoardMutRecord(String id) {
        super(id);
    }

    public BoardMutRecord(BoardDef boardDef) {
        super(boardDef);
    }

    @JsonProperty("_content")
    public void setContent(List<ObjectData> content) {
        String dataUriContent = content.get(0).get("url", "");
        ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);
        Json.getMapper().applyData(this, data);
    }
}
