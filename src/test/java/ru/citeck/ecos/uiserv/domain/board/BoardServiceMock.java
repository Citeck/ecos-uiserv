package ru.citeck.ecos.uiserv.domain.board;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.service.BoardMapper;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BoardServiceMock implements BoardService {

    ConcurrentHashMap<String, BoardEntity> data = new ConcurrentHashMap<>();
    private final List<BiConsumer<BoardDef, BoardDef>> changeListeners = new CopyOnWriteArrayList<>();
    PredicateService predicateService;

    public BoardServiceMock(RecordsServiceFactory recordsServiceFactory) {
        predicateService = recordsServiceFactory.getPredicateService();
    }

    @Override
    public void delete(String id) {
        data.remove(id);
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
    public List<BoardWithMeta> getAll(int maxItems, int skipCount, Predicate predicate, Sort sort) {
        return predicateService.filter(data.values(), predicate)
            .stream().map(BoardMapper::entityToDto).collect(Collectors.toList());
    }

    @Override
    public BoardWithMeta getBoardById(String id) {
        if (StringUtils.isBlank(id) || data.get(id) == null) {
            return null;
        }
        return BoardMapper.entityToDto(data.get(id));
    }

    @Override
    public long getCount(Predicate predicate) {
        return this.getAll(0, 0, predicate, null).size();
    }

    @Override
    public long getCount() {
        return data.size();
    }

    @Override
    public List<BoardWithMeta> getBoardsForJournal(RecordRef journalRef) {
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
    public List<BoardWithMeta> getBoardsForExactType(RecordRef typeRef, Sort sort) {
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
