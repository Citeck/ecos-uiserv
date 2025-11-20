package ru.citeck.ecos.uiserv.domain.board.api.records;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.model.lib.utils.ModelUtils;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.records3.record.atts.schema.ScalarType;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.nio.charset.StandardCharsets;

/**
 * Class supports uploading board from yml-file,
 * editing board with JSON Editor
 *
 */
@Data
public class BoardRecord {
    @AttName("...")
    private BoardWithMeta boardDefWithMeta;
    private WorkspaceService workspaceService;

    public BoardRecord(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public BoardRecord(BoardWithMeta boardDefWithMeta, WorkspaceService workspaceService) {
        this.boardDefWithMeta = boardDefWithMeta;
        this.workspaceService = workspaceService;
    }

    public BoardRecord(String id, WorkspaceService workspaceService) {
        boardDefWithMeta = new BoardWithMeta(id);
        this.workspaceService = workspaceService;
    }

    @JsonIgnore
    @AttName(ScalarType.ID_SCHEMA)
    public EntityRef getRef() {
        BoardDef boardDef = boardDefWithMeta.getBoardDef();
        var localId = workspaceService.addWsPrefixToId(boardDef.getId(), boardDef.getWorkspace());
        return EntityRef.create(Application.NAME, BoardRecordsDao.ID, localId);
    }

    public BoardDef getBoardDef() {
        return boardDefWithMeta != null ? boardDefWithMeta.getBoardDef() : null;
    }

    @JsonValue
    public JsonNode toNonDefaultJson() {
        return Json.getMapper().toNonDefaultJson(boardDefWithMeta.getBoardDef());
    }

    public byte[] getData() {
        BoardDef boardDefCopy = new BoardDef(boardDefWithMeta.getBoardDef());
        boardDefCopy.setTypeRef(prepareExtRef(boardDefCopy.getTypeRef()));
        boardDefCopy.setJournalRef(prepareExtRef(boardDefCopy.getJournalRef()));
        boardDefCopy.setCardFormRef(prepareExtRef(boardDefCopy.getCardFormRef()));

        return YamlUtils.toNonDefaultString(boardDefCopy).getBytes(StandardCharsets.UTF_8);
    }

    private EntityRef prepareExtRef(EntityRef ref) {
        return ref.withLocalId(workspaceService.replaceWsPrefixToCurrentWsPlaceholder(ref.getLocalId()));
    }

    public String getEcosType() {
        return "board";
    }

    public Permissions getPermissions() {
        return new Permissions();
    }

    public class Permissions implements AttValue {

        @Override
        public boolean has(@NotNull String name) {
            if (name.equalsIgnoreCase("write")) {
                BoardDef boardDef = boardDefWithMeta != null ? boardDefWithMeta.getBoardDef() : null;
                String workspace = boardDef != null && boardDef.getWorkspace() != null
                    ? boardDef.getWorkspace()
                    : ModelUtils.DEFAULT_WORKSPACE_ID;
                return AuthContext.isRunAsAdmin() || workspaceService.isUserManagerOf(AuthContext.getCurrentUser(), workspace);
            } else {
                return name.equalsIgnoreCase("read");
            }
        }
    }
}
