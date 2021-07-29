package ru.citeck.ecos.uiserv.domain.board.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository repository;

    @Override
    public Optional<BoardDef> getBoardById(String id) {
        if (StringUtils.isBlank(id)) {
            return Optional.empty();
        }
        return repository.findByExtId(id).map(BoardMapper::entityToDto);
    }

    @Override
    public String save(BoardDef boardDef) {
        BoardEntity entity = repository.save(BoardMapper.dtoToEntity(repository, boardDef));
        BoardDef result = BoardMapper.entityToDto(entity);

        //listeners.forEach(it -> it.accept(result));

        return result.getId();
    }

    @Override
    public List<BoardDef> getBordsForExactType(RecordRef typeRef) {
        List<BoardEntity> boardEntities = repository.findAllByTypeRef(typeRef.toString());
        return boardEntities.stream().map(BoardMapper::entityToDto).collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        repository.findByExtId(id).ifPresent(repository::delete);
    }
}
