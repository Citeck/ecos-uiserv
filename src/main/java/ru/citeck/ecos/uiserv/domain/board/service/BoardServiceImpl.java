package ru.citeck.ecos.uiserv.domain.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[\\w-]+$");

    private final BoardRepository repository;
    private final List<BiConsumer<BoardDef, BoardDef>> changeListeners = new CopyOnWriteArrayList<>();

    private final JpaSearchConverterFactory jpaSearchConverterFactory;
    private JpaSearchConverter<BoardEntity> searchConv;

    @PostConstruct
    public void init() {
        searchConv = jpaSearchConverterFactory.createConverter(BoardEntity.class).build();
    }

    @Override
    public BoardWithMeta getBoardById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return repository.findByExtId(id)
            .map(BoardMapper::entityToDto)
            .orElse(null);
    }

    @Override
    @Transactional
    public BoardWithMeta save(BoardDef boardDef) {

        Assert.notNull(boardDef, "Board must not be null");
        if (!StringUtils.isEmpty(boardDef.getId())) {
            if (!VALID_ID_PATTERN.matcher(boardDef.getId()).matches()) {
                throw new IllegalArgumentException("Invalid ID: '" + boardDef.getId() + "'");
            }
        }

        BoardDef beforeBoardDef = Optional.ofNullable(getBoardById(boardDef.getId()))
            .map(BoardWithMeta::getBoardDef)
            .orElse(null);

        BoardEntity entity = repository.save(BoardMapper.dtoToEntity(repository, boardDef));
        BoardWithMeta result = BoardMapper.entityToDto(entity);

        for (BiConsumer<BoardDef, BoardDef> listener : changeListeners) {
            listener.accept(beforeBoardDef, result.getBoardDef());
        }
        return result;
    }

    @Override
    public List<BoardWithMeta> getBoardsForExactType(RecordRef typeRef, Sort sort) {
        Assert.notNull(typeRef, "To select boards typeRef must not be null");
        List<BoardEntity> boardEntities = repository.findAllByTypeRef(typeRef.toString(), sort);
        return boardEntities.stream().map(BoardMapper::entityToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(String id) {
        Assert.notNull(id, "Deleted board ID must not be null");
        repository.findByExtId(id).ifPresent(repository::delete);
    }

    @Override
    public List<BoardWithMeta> getAll(Predicate predicate, int maxItems, int skipCount, List<SortBy> sort) {
        return searchConv.findAll(repository, predicate, maxItems, skipCount, sort)
            .stream()
            .map(BoardMapper::entityToDto)
            .collect(Collectors.toList());
    }

    @Override
    public long getCount() {
        return repository.count();
    }

    @Override
    public long getCount(Predicate predicate) {
        return searchConv.getCount(repository, predicate);
    }

    @Override
    public void onBoardChanged(BiConsumer<BoardDef, BoardDef> listener) {
        changeListeners.add(listener);
    }

    @Override
    public List<BoardWithMeta> getBoardsForJournal(RecordRef journalRef) {
        Assert.notNull(journalRef, "To select boards journalRef must not be null");
        return repository.findAllByJournalRef(journalRef.toString(), Sort.by(Sort.Direction.DESC, BoardEntity.ID))
            .stream().map(BoardMapper::entityToDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<BoardWithMeta> getBoardsForJournal(String journalLocalId) {
        Assert.notNull(journalLocalId, "To select boards journal local ID must not be null");
        return getBoardsForJournal(RecordRef.create(Application.NAME, "journal", journalLocalId));
    }
}
