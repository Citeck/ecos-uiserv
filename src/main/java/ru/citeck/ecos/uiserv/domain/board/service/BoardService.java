package ru.citeck.ecos.uiserv.domain.board.service;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;

import java.util.List;
import java.util.Optional;

public interface BoardService {

    Optional<BoardDef> getBoardById(String id);

    void delete(@NotNull String id);

    List<BoardDef> getBordsForExactType(@NotNull RecordRef typeRef);

    String save(@NotNull BoardDef boardDef);
}
