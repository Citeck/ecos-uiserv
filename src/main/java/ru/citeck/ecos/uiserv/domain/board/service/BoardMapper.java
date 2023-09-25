package ru.citeck.ecos.uiserv.domain.board.service;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;

import java.util.UUID;

public class BoardMapper {

    public static BoardWithMeta entityToDto(@NotNull BoardEntity entity) {

        BoardDef boardDto = new BoardDef();
        boardDto.setId(entity.getExtId());
        boardDto.setReadOnly(entity.getReadOnly());
        boardDto.setDisableTitle(entity.getDisableTitle());
        boardDto.setName(Json.getMapper().read(entity.getName(), MLText.class));
        if (entity.getTypeRef() != null) {
            boardDto.setTypeRef(RecordRef.valueOf(entity.getTypeRef()));
        }
        if (entity.getJournalRef() != null) {
            boardDto.setJournalRef(RecordRef.valueOf(entity.getJournalRef()));
        }
        if (entity.getCardFormRef() != null) {
            boardDto.setCardFormRef(RecordRef.valueOf(entity.getCardFormRef()));
        }
        if (entity.getColumns() != null) {
            boardDto.setColumns(Json.getMapper().readList(entity.getColumns(), BoardColumnDef.class));
        }
        if (entity.getActions() != null) {
            boardDto.setActions(Json.getMapper().readList(entity.getActions(), RecordRef.class));
        }
        BoardWithMeta boardWithMeta = new BoardWithMeta();
        boardWithMeta.setBoardDef(boardDto);
        boardWithMeta.setModifier(entity.getLastModifiedBy());
        boardWithMeta.setModified(entity.getLastModifiedDate());
        boardWithMeta.setCreator(entity.getCreatedBy());
        boardWithMeta.setCreated(entity.getCreatedDate());
        return boardWithMeta;
    }

    public static BoardEntity dtoToEntity(BoardRepository repository, @NotNull BoardDef board) {

        BoardEntity entity = null;
        if (repository != null && !StringUtils.isBlank(board.getId())) {
            entity = repository.findByExtId(board.getId()).orElse(null);
        }
        if (entity == null) {
            entity = new BoardEntity();
            if (StringUtils.isBlank(board.getId())) {
                entity.setExtId(UUID.randomUUID().toString());
            } else {
                entity.setExtId(board.getId());
            }
        }
        entity.setName(Json.getMapper().toString(board.getName()));
        entity.setReadOnly(board.getReadOnly());
        entity.setDisableTitle(board.getDisableTitle());

        entity.setTypeRef(RecordRef.isNotEmpty(board.getTypeRef()) ? RecordRef.toString(board.getTypeRef()) : null);

        entity.setJournalRef(RecordRef.isNotEmpty(board.getJournalRef()) ? RecordRef.toString(board.getJournalRef()) : null);

        if (RecordRef.isNotEmpty(board.getCardFormRef())) {
            entity.setCardFormRef(RecordRef.toString(board.getCardFormRef()));
        } else {
            entity.setCardFormRef(null);
        }
        entity.setActions(Json.getMapper().toString(board.getActions()));
        entity.setColumns(Json.getMapper().toString(board.getColumns()));
        return entity;
    }
}
