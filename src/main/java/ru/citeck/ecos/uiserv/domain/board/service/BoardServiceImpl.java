package ru.citeck.ecos.uiserv.domain.board.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo;
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    private static final Logger log = LoggerFactory.getLogger(BoardServiceImpl.class);
    private final EcosTypeService typeService;
    private final BoardRepository repository;
    private final List<Consumer<BoardDef>> changeListeners = new CopyOnWriteArrayList<>();

    @Override
    public Optional<BoardWithMeta> getBoardById(String id) {
        if (StringUtils.isBlank(id)) {
            return Optional.empty();
        }
        return repository.findByExtId(id).map(BoardMapper::entityToDto);
    }

    @Override
    @Transactional
    public BoardWithMeta save(BoardDef boardDef) {
        Assert.notNull(boardDef, "Board must not be null");
        /*if (boardDef.getTypeRef() != null && boardDef.getColumns() != null && !boardDef.getColumns().isEmpty()) {
            EcosTypeInfo typeInfo = typeService.getTypeInfo(boardDef.getTypeRef());
            if (typeInfo != null && typeInfo.getModel() != null && typeInfo.getModel().getStatuses() != null) {
                final Set<String> typeStatuses = typeInfo.getModel().getStatuses().stream()
                    .map(statusDef -> statusDef.getId()).collect(Collectors.toSet());
                boardDef.getColumns().stream()
                    .forEach(boardColumnDef -> {
                        if (!typeStatuses.contains(boardColumnDef.getId()))
                            log.warn("Unknown status '{}' for type '{}'", boardColumnDef.getId(), typeInfo.getId());
                    });
            }
        }*/
        BoardEntity entity = repository.save(BoardMapper.dtoToEntity(repository, boardDef));
        BoardWithMeta result = BoardMapper.entityToDto(entity);

        changeListeners.forEach(listener -> listener.accept(result.getBoardDef()));
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
    public void onBoardChanged(Consumer<BoardDef> listener) {
        if (listener != null)
            changeListeners.add(listener);
    }

    /* Also common part with JournalServiceImpl -> common interface*/
    private Specification<BoardEntity> toSpecification(Predicate predicate) {
        if (predicate == null)
            return null;
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
