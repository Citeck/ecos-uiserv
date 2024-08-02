package ru.citeck.ecos.uiserv.domain.board.service;

import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Sort;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.function.BiConsumer;

public interface BoardService {

    @Nullable BoardWithMeta getBoardById(String id);

    void delete(String id);

    List<BoardWithMeta> getBoardsForExactType(EntityRef typeRef, Sort sort);

    BoardWithMeta save(BoardDef boardDef);

    long getCount();

    long getCount(Predicate predicate);

    List<BoardWithMeta> getAll(Predicate predicate, int maxItems, int skipCount, List<SortBy> sort);

    void onBoardChanged(BiConsumer<BoardDef, BoardDef> listener);

    List<BoardWithMeta> getBoardsForJournal(EntityRef journalRef);

    List<BoardWithMeta> getBoardsForJournal(String journalLocalId);
}
