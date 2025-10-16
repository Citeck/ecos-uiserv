package ru.citeck.ecos.uiserv.domain.board;

import org.springframework.data.domain.Sort;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.service.BoardMapper;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class BoardServiceMock implements BoardService {

    ConcurrentHashMap<String, BoardEntity> data = new ConcurrentHashMap<>();
    private final List<BiConsumer<BoardDef, BoardDef>> changeListeners = new CopyOnWriteArrayList<>();
    PredicateService predicateService;

    public BoardServiceMock(RecordsServiceFactory recordsServiceFactory) {
        predicateService = recordsServiceFactory.getPredicateService();
    }

    @Override
    public void delete(IdInWs id) {
        data.remove(id.getId());
    }

    @Override
    public BoardWithMeta save(BoardDef boardDef) {

        BoardEntity entityBefore = data.get(boardDef.getId());
        BoardDef valueBefore = null;
        if (entityBefore != null) {
            valueBefore = BoardMapper.entityToDto(entityBefore).getBoardDef();
        }

        BoardEntity entity = BoardMapper.dtoToEntity(null, boardDef);
        data.put(boardDef.getId(), entity);
        BoardWithMeta boardWithMeta = BoardMapper.entityToDto(entity);

        for (BiConsumer<BoardDef, BoardDef> listener : changeListeners) {
            listener.accept(valueBefore, boardWithMeta.getBoardDef());
        }
        return boardWithMeta;
    }

    @Override
    public List<BoardWithMeta> getAll(Predicate predicate, List<String> workspaces, int maxItems, int skipCount, List<SortBy> sort) {
        return predicateService.filter(data.values(), predicate)
            .stream().map(BoardMapper::entityToDto).collect(Collectors.toList());
    }

    @Override
    public BoardWithMeta getBoardById(IdInWs id) {
        if (id.isEmpty() || data.get(id.getId()) == null) {
            return null;
        }
        return BoardMapper.entityToDto(data.get(id.getId()));
    }

    @Override
    public long getCount(Predicate predicate) {
        return this.getAll(predicate, Collections.emptyList(), 0, 0, null).size();
    }

    @Override
    public long getCount() {
        return data.size();
    }

    @Override
    public List<BoardWithMeta> getBoardsForJournal(EntityRef journalRef) {
        if (journalRef == null) {
            return null;
        }
        return data.values().stream()
            .filter(boardEntity -> journalRef.toString().equals(boardEntity.getJournalRef()))
            .map(BoardMapper::entityToDto).collect(Collectors.toList());
    }

    @Override
    public List<BoardWithMeta> getBoardsForJournal(String journalLocalId) {
        if (journalLocalId == null) {
            return null;
        }
        return data.values().stream()
            .filter(boardEntity -> {
                String journalRefStr = boardEntity.getJournalRef();
                int idx = journalRefStr.indexOf('@');
                String entityJournalLocalId = idx==-1?journalRefStr:
                    journalRefStr.substring(idx+1);
                return journalLocalId.equals(entityJournalLocalId);
            })
            .map(BoardMapper::entityToDto).collect(Collectors.toList());
    }

    @Override
    public List<BoardWithMeta> getBoardsForExactType(EntityRef typeRef, Sort sort) {
        if (typeRef == null)
            return null;
        return data.values().stream()
            .filter(boardEntity -> typeRef.toString().equals(boardEntity.getJournalRef()))
            .map(BoardMapper::entityToDto).collect(Collectors.toList());
    }

    @Override
    public void onBoardChanged(BiConsumer<BoardDef, BoardDef> listener) {
        changeListeners.add(listener);
    }
}
