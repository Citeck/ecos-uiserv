package ru.citeck.ecos.uiserv.domain.board.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class BoardArtifactHandler implements WsAwareArtifactHandler<BoardDef> {
    final private BoardService service;

    @Override
    public void deployArtifact(@NotNull BoardDef boardDef, @NotNull String workspace) {
        boardDef = new BoardDef(boardDef);
        boardDef.setWorkspace(workspace);
        service.save(boardDef);
    }

    @Override
    public void listenChanges(@NotNull BiConsumer<BoardDef, String> listener) {
        service.onBoardChanged((before, after) -> {
            String workspace = after.getWorkspace() != null ? after.getWorkspace() : "";
            BoardDef stripped = new BoardDef(after);
            stripped.setWorkspace("");
            listener.accept(stripped, workspace);
        });
    }

    @Override
    public void deleteArtifact(@NotNull String artifactId, @NotNull String workspace) {
        service.delete(IdInWs.create(workspace, artifactId));
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/board";
    }
}
