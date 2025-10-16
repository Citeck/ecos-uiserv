package ru.citeck.ecos.uiserv.domain.board.service;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.model.lib.utils.ModelUtils;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.UUID;

public class BoardMapper {

    public static BoardWithMeta entityToDto(@NotNull BoardEntity entity) {

        BoardDef boardDto = new BoardDef();
        boardDto.setId(entity.getExtId());
        boardDto.setWorkspace(StringUtils.defaultIfBlank(entity.getWorkspace(), ModelUtils.DEFAULT_WORKSPACE_ID));
        boardDto.setReadOnly(entity.getReadOnly());
        boardDto.setDisableTitle(entity.getDisableTitle());
        boardDto.setName(Json.getMapper().read(entity.getName(), MLText.class));
        if (entity.getTypeRef() != null) {
            boardDto.setTypeRef(EntityRef.valueOf(entity.getTypeRef()));
        }
        if (entity.getJournalRef() != null) {
            boardDto.setJournalRef(EntityRef.valueOf(entity.getJournalRef()));
        }
        if (entity.getCardFormRef() != null) {
            boardDto.setCardFormRef(EntityRef.valueOf(entity.getCardFormRef()));
        }
        if (entity.getCardTitleTemplate() != null) {
            boardDto.setCardTitleTemplate(entity.getCardTitleTemplate());
        }
        if (entity.getColumns() != null) {
            boardDto.setColumns(Json.getMapper().readList(entity.getColumns(), BoardColumnDef.class));
        }
        if (entity.getActions() != null) {
            boardDto.setActions(Json.getMapper().readList(entity.getActions(), EntityRef.class));
        }
        BoardDef.CardFieldsLabelLayout layout = BoardDef.DEFAULT_CARD_FIELDS_LABEL_LAYOUT;
        if (StringUtils.isNotBlank(entity.getCardFieldsLabelLayout())) {
            try {
                layout = BoardDef.CardFieldsLabelLayout.valueOf(entity.getCardFieldsLabelLayout());
            } catch (Throwable e) {
                // do nothing
            }
        }
        boardDto.setCardFieldsLabelLayout(layout);

        BoardWithMeta boardWithMeta = new BoardWithMeta();
        boardWithMeta.setBoardDef(boardDto);
        boardWithMeta.setModifier(entity.getLastModifiedBy());
        boardWithMeta.setModified(entity.getLastModifiedDate());
        boardWithMeta.setCreator(entity.getCreatedBy());
        boardWithMeta.setCreated(entity.getCreatedDate());
        return boardWithMeta;
    }

    public static BoardEntity dtoToEntity(BoardRepository repository, @NotNull BoardDef board) {

        String workspace = board.getWorkspace();
        if (ModelUtils.DEFAULT_WORKSPACE_ID.equals(workspace)) {
            workspace = "";
        }

        BoardEntity entity = null;
        if (repository != null && !StringUtils.isBlank(board.getId())) {
            entity = repository.findByExtIdAndWorkspace(board.getId(), workspace).orElse(null);
        }
        if (entity == null) {
            entity = new BoardEntity();
            if (StringUtils.isBlank(board.getId())) {
                entity.setExtId(UUID.randomUUID().toString());
            } else {
                entity.setExtId(board.getId());
            }
        }

        entity.setWorkspace(workspace);
        entity.setName(Json.getMapper().toString(board.getName()));
        entity.setReadOnly(board.getReadOnly());
        entity.setDisableTitle(board.getDisableTitle());
        if (board.getCardFieldsLabelLayout() != null) {
            entity.setCardFieldsLabelLayout(board.getCardFieldsLabelLayout().name());
        }

        entity.setTypeRef(EntityRef.isNotEmpty(board.getTypeRef()) ? EntityRef.toString(board.getTypeRef()) : null);

        entity.setJournalRef(EntityRef.isNotEmpty(board.getJournalRef()) ? EntityRef.toString(board.getJournalRef()) : null);

        if (EntityRef.isNotEmpty(board.getCardFormRef())) {
            entity.setCardFormRef(EntityRef.toString(board.getCardFormRef()));
        } else {
            entity.setCardFormRef(null);
        }
        entity.setCardTitleTemplate(board.getCardTitleTemplate());

        entity.setActions(Json.getMapper().toString(board.getActions()));
        entity.setColumns(Json.getMapper().toString(board.getColumns()));
        return entity;
    }
}
