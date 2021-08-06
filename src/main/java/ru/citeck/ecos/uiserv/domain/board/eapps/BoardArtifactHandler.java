package ru.citeck.ecos.uiserv.domain.board.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class BoardArtifactHandler implements EcosArtifactHandler<BoardDef> {
    final private BoardService service;

    @Override
    public void deleteArtifact(@NotNull String id) {
        service.delete(id);
    }

    @Override
    public void deployArtifact(@NotNull BoardDef boardDef) {
        service.save(boardDef);
    }

    @Override
    public void listenChanges(@NotNull Consumer<BoardDef> consumer) {
        service.onBoardChanged(consumer);
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/board";
    }
}
