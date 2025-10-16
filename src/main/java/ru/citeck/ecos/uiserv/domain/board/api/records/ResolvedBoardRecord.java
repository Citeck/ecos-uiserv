package ru.citeck.ecos.uiserv.domain.board.api.records;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.model.lib.status.dto.StatusDef;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResolvedBoardRecord {

    @AttName("...")
    private final BoardDef boardDef;
    private final EcosTypeService typeService;
    private final WorkspaceService workspaceService;

    public static final String ID = "rboard";

    public ResolvedBoardRecord(BoardDef boardDef, EcosTypeService typeService, WorkspaceService workspaceService) {
        this.boardDef = boardDef;
        this.typeService = typeService;
        this.workspaceService = workspaceService;
    }

    public MLText getName() {
        if (boardDef != null) {
            if (MLText.isEmpty(boardDef.getName())) {
                return new MLText(boardDef.getId());
            }
            return boardDef.getName();
        }
        return MLText.EMPTY;
    }

    public String getId() {
        if (boardDef.getId().startsWith("type$")) {
            return boardDef.getId();
        }
        return workspaceService.addWsPrefixToId(boardDef.getId(), boardDef.getWorkspace());
    }

    public BoardDef getBoardDef() {
        return boardDef;
    }

    public List<BoardColumnDef> getColumns() {
        if (boardDef == null) {
            return Collections.emptyList();
        }
        if (!CollectionUtils.isEmpty(boardDef.getColumns())) {
            return Collections.unmodifiableList(boardDef.getColumns());
        }
        TypeDef typeInfo = typeService.getTypeInfo(boardDef.getTypeRef());
        if (typeInfo != null) {
            List<BoardColumnDef> columns = new ArrayList<>();
            for (StatusDef statusDef : typeInfo.getModel().getStatuses()) {
                columns.add(
                    BoardColumnDef.create()
                        .withId(statusDef.getId())
                        .withName(statusDef.getName())
                        .build()
                );
            }
            return columns;
        }
        return new ArrayList<>();
    }

    public EntityRef getTypeRef() {

        if (boardDef == null) {
            return EntityRef.EMPTY;
        }
        if (EntityRef.isNotEmpty(boardDef.getTypeRef())) {
            return boardDef.getTypeRef();
        }
        return typeService.getTypeRefByBoard(IdInWs.create(boardDef.getId(), boardDef.getWorkspace()));
    }

    public EntityRef getJournalRef() {

        if (boardDef == null) {
            return EntityRef.EMPTY;
        }
        if (EntityRef.isNotEmpty(boardDef.getJournalRef())) {
            return boardDef.getJournalRef();
        }
        EntityRef typeRef = getTypeRef();
        if (EntityRef.isNotEmpty(typeRef)) {
            TypeDef typeInfo = typeService.getTypeInfo(typeRef);
            if (typeInfo != null) {
                return EntityRef.valueOf(typeInfo.getJournalRef());
            }
        }
        return EntityRef.EMPTY;
    }

    public EntityRef getCardFormRef() {
        if (boardDef != null && boardDef.getCardFormRef() != null
            && !StringUtils.isBlank(boardDef.getCardFormRef().getLocalId())) {
            return EntityRef.create(boardDef.getCardFormRef().getAppName(), ID, boardDef.getCardFormRef().getLocalId());
        }
        return EntityRef.valueOf("uiserv/form@board-card-default");
    }

    public String getEcosType() {
        return "board";
    }
}
