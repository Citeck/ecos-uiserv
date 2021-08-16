package ru.citeck.ecos.uiserv.domain.board.service;

import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Sort;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;

import java.util.List;
import java.util.function.Consumer;

public interface BoardService {

    @Nullable BoardWithMeta getBoardById(String id);

    void delete(String id);

    List<BoardWithMeta> getBoardsForExactType(RecordRef typeRef, Sort sort);

    BoardWithMeta save(BoardDef boardDef);

    long getCount();

    long getCount(Predicate predicate);

    List<BoardWithMeta> getAll(int maxItems, int skipCount, Predicate predicate, Sort sort);

    void onBoardChanged(Consumer<BoardDef> listener);

    List<BoardWithMeta> getBoardsForJournal(RecordRef journalRef);

    List<BoardWithMeta> getBoardsForJournal(String journalLocalId);
}
