package ru.citeck.ecos.uiserv.domain.board.api.records;

import org.apache.commons.collections.CollectionUtils;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.model.lib.status.dto.StatusDef;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo;
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResolvedBoardRecord {
    //name, typeRef, cardFormRef, columns
    @AttName("...")
    private BoardDef boardDef;
    private BoardService service;
    private EcosTypeService typeService;
    String defultCardFormId;

    public ResolvedBoardRecord(BoardDef boardDef, BoardService service, EcosTypeService typeService) {
        this.boardDef = boardDef;
        this.service = service;
        this.typeService = typeService;
    }

    public MLText getName() {
        if (boardDef != null) {
            if (MLText.isEmpty(boardDef.getName())) {
                return new MLText(boardDef.getId());
            }
        }
        return MLText.EMPTY;
    }

    public List<BoardColumnDef> getColumns() {
        if (boardDef != null) {
            if (!CollectionUtils.isEmpty(boardDef.getColumns())) {
                return Collections.unmodifiableList(boardDef.getColumns());
            }
            if (typeService != null) {
                EcosTypeInfo typeInfo = typeService.getTypeInfo(boardDef.getTypeRef());
                if (typeInfo != null && typeInfo.getModel() != null && typeInfo.getModel().getStatuses() != null) {
                    List<BoardColumnDef> columns = new ArrayList<>();
                    for (StatusDef statusDef : typeInfo.getModel().getStatuses()) {
                        columns.add(new BoardColumnDef(statusDef.getId(), statusDef.getName()));
                    }
                    return columns;
                }
            }
        }
        return new ArrayList<BoardColumnDef>();
    }
}
