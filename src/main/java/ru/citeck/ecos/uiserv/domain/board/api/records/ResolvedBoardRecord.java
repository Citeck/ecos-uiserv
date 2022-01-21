package ru.citeck.ecos.uiserv.domain.board.api.records;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.model.lib.status.dto.StatusDef;
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo;
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResolvedBoardRecord {

    @AttName("...")
    private final BoardDef boardDef;
    private final EcosTypeService typeService;

    public static final String ID = "rboard";

    public ResolvedBoardRecord(BoardDef boardDef, EcosTypeService typeService) {
        this.boardDef = boardDef;
        this.typeService = typeService;
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
        EcosTypeInfo typeInfo = typeService.getTypeInfo(boardDef.getTypeRef());
        if (typeInfo != null && typeInfo.getModel() != null) {
            List<BoardColumnDef> columns = new ArrayList<>();
            for (StatusDef statusDef : typeInfo.getModel().getStatuses()) {
                columns.add(new BoardColumnDef(statusDef.getId(), statusDef.getName()));
            }
            return columns;
        }
        return new ArrayList<>();
    }

    public RecordRef getTypeRef() {

        if (boardDef == null) {
            return RecordRef.EMPTY;
        }
        if (RecordRef.isNotEmpty(boardDef.getTypeRef())) {
            return boardDef.getTypeRef();
        }
        return typeService.getTypeRefByBoard(boardDef.getId());
    }

    public RecordRef getJournalRef() {

        if (boardDef == null) {
            return RecordRef.EMPTY;
        }
        if (RecordRef.isNotEmpty(boardDef.getJournalRef())) {
            return boardDef.getJournalRef();
        }
        RecordRef typeRef = getTypeRef();
        if (RecordRef.isNotEmpty(typeRef)) {
            EcosTypeInfo typeInfo = typeService.getTypeInfo(typeRef);
            if (typeInfo != null) {
                return typeInfo.getJournalRef();
            }
        }
        return RecordRef.EMPTY;
    }

    public RecordRef getCardFormRef() {
        if (boardDef != null && boardDef.getCardFormRef() != null
            && !StringUtils.isBlank(boardDef.getCardFormRef().getId())) {
            return RecordRef.create(boardDef.getCardFormRef().getAppName(), ID, boardDef.getCardFormRef().getId());
        }
        return RecordRef.valueOf("uiserv/form@board-card-default");
    }

    public RecordRef getEcosType() {
        return TypeUtils.getTypeRef("board");
    }
}
