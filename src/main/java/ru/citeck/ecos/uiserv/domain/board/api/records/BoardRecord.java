package ru.citeck.ecos.uiserv.domain.board.api.records;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;

import java.nio.charset.StandardCharsets;

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
    public JsonNode toNonDefaultJson() {
        return Json.getMapper().toNonDefaultJson(boardDefWithMeta.getBoardDef());
    }

    public byte[] getData() {
        return YamlUtils.toNonDefaultString(toNonDefaultJson()).getBytes(StandardCharsets.UTF_8);
    }

    public String getEcosType() {
        return "board";
    }
}
