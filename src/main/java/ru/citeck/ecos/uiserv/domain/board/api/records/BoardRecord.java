package ru.citeck.ecos.uiserv.domain.board.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
* Class supports uploading board from yml-file,
* editing board with JSON Editor
* */
@Data
public class BoardRecord {
    @AttName("...")
    private BoardWithMeta boardDefWithMeta;

    public BoardRecord() {
    }

    public BoardRecord(BoardWithMeta boardDefWithMeta) {
        this.boardDefWithMeta = boardDefWithMeta;
    }

    public BoardRecord(String id) {
        boardDefWithMeta = new BoardWithMeta(id);
    }

    public BoardDef getBoardDef() {
        return boardDefWithMeta != null ? boardDefWithMeta.getBoardDef() : null;
    }

    @JsonValue
    @com.fasterxml.jackson.annotation.JsonValue
    public JsonNode toNonDefaultJson() {
        return Json.getMapper().toNonDefaultJson(boardDefWithMeta.getBoardDef());
    }

    public byte[] getData() {
        return YamlUtils.toNonDefaultString(toNonDefaultJson()).getBytes(StandardCharsets.UTF_8);
    }

    @JsonProperty("_content")
    public void setContent(List<ObjectData> content) {
        String dataUriContent = content.get(0).get("url", "");
        ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);
        Json.getMapper().applyData(getBoardDef(), data);
    }
}
