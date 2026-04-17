package ru.citeck.ecos.uiserv.domain.board.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardArtifactHandler implements WsAwareArtifactHandler<BoardDef> {
    final private BoardService service;
    final private WorkspaceService workspaceService;

    @Override
    public void deployArtifact(@NotNull BoardDef boardDef, @NotNull String workspace) {
        BoardDef copy = new BoardDef(boardDef);
        copy.setWorkspace(workspace);
        applyRefs(copy, ref ->
            ref.withLocalId(workspaceService.replaceCurrentWsPlaceholderToWsPrefix(ref.getLocalId(), workspace))
        );
        service.save(copy);
    }

    @Override
    public void listenChanges(@NotNull BiConsumer<BoardDef, String> listener) {
        service.onBoardChanged((before, after) -> {
            String workspace = after.getWorkspace() != null ? after.getWorkspace() : "";
            BoardDef stripped = new BoardDef(after);
            stripped.setWorkspace("");
            applyRefs(stripped, ref ->
                ref.withLocalId(workspaceService.replaceWsPrefixToCurrentWsPlaceholder(ref.getLocalId()))
            );
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

    private void applyRefs(BoardDef board, Function<EntityRef, EntityRef> transform) {
        if (EntityRef.isNotEmpty(board.getTypeRef())) {
            board.setTypeRef(transform.apply(board.getTypeRef()));
        }
        if (EntityRef.isNotEmpty(board.getJournalRef())) {
            board.setJournalRef(transform.apply(board.getJournalRef()));
        }
        if (EntityRef.isNotEmpty(board.getCardFormRef())) {
            board.setCardFormRef(transform.apply(board.getCardFormRef()));
        }
        List<EntityRef> actions = board.getActions();
        if (actions != null && !actions.isEmpty()) {
            board.setActions(actions.stream()
                .map(ref -> EntityRef.isNotEmpty(ref) ? transform.apply(ref) : ref)
                .collect(Collectors.toList()));
        }
    }
}
