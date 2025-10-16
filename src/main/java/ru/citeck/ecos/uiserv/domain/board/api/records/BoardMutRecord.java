package ru.citeck.ecos.uiserv.domain.board.api.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;

import java.util.List;

public class BoardMutRecord extends BoardDef {

    private final WorkspaceService workspaceService;

    public BoardMutRecord(String id, WorkspaceService workspaceService) {
        super(id);
        this.workspaceService = workspaceService;
    }

    public BoardMutRecord(BoardDef boardDef, WorkspaceService workspaceService) {
        super(boardDef);
        this.workspaceService = workspaceService;
    }

    @JsonProperty(RecordConstants.ATT_WORKSPACE)
    public void setCtxWorkspace(String workspace) {
        this.setWorkspace(
            workspaceService.getUpdatedWsInMutation(StringUtils.defaultString(getWorkspace()), workspace)
        );
    }

    @JsonProperty("_content")
    public void setContent(List<ObjectData> content) {
        String dataUriContent = content.getFirst().get("url", "");
        ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);
        Json.getMapper().applyData(this, data);
    }
}
