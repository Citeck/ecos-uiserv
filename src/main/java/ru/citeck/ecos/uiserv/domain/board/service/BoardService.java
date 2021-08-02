package ru.citeck.ecos.uiserv.domain.board.service;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface BoardService {

    Optional<BoardWithMeta> getBoardById(String id);

    void delete(String id);

    List<BoardWithMeta> getBoardsForExactType(RecordRef typeRef);

    BoardWithMeta save(BoardDef boardDef);

    long getCount();

    long getCount(Predicate predicate);

    List<BoardWithMeta> getAll(int maxItems, int skipCount, Predicate predicate);

    void onBoardChanged(Consumer<BoardDef> listener);
}
