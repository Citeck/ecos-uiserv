package ru.citeck.ecos.uiserv.domain.board.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.app.application.constants.AppConstants;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository repository;
    private final List<BiConsumer<BoardDef, BoardDef>> changeListeners = new CopyOnWriteArrayList<>();

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
    public List<BoardWithMeta> getAll(int maxItems, int skipCount, Predicate predicate, Sort sort) {
        if (maxItems == 0) {
            return Collections.emptyList();
        }
        final PageRequest page = PageRequest.of(skipCount / maxItems, maxItems,
            sort != null ? sort : Sort.by(Sort.Direction.DESC, BoardEntity.ID)
        );
        return repository.findAll(toSpecification(predicate), page)
            .stream().map(BoardMapper::entityToDto)
            .collect(Collectors.toList());
    }

    @Override
    public long getCount() {
        return repository.count();
    }

    @Override
    public long getCount(Predicate predicate) {
        return repository.count(toSpecification(predicate));
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
        return getBoardsForJournal(RecordRef.create(AppConstants.APP_NAME, "journal", journalLocalId));
    }

    private Specification<BoardEntity> toSpecification(Predicate predicate) {
        if (predicate == null) {
            return null;
        }
        PredicateDto predicateDto = PredicateUtils.convertToDto(predicate, PredicateDto.class);
        Specification<BoardEntity> specification = null;
        if (StringUtils.isNotBlank(predicateDto.name)) {
            specification = (root, query, builder) ->
                builder.like(builder.lower(root.get("name")), "%" + predicateDto.name.toLowerCase() + "%");
        }
        if (StringUtils.isNotBlank(predicateDto.localId)) {
            Specification<BoardEntity> idSpecification = (root, query, builder) ->
                builder.like(builder.lower(root.get("extId")), "%" + predicateDto.localId.toLowerCase() + "%");
            specification = specification != null ? specification.or(idSpecification) : idSpecification;
        }

        return specification;
    }

    @Data
    public static class PredicateDto {
        private String name;
        private String localId;
    }
}
