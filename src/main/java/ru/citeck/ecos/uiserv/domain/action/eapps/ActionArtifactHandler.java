package ru.citeck.ecos.uiserv.domain.action.eapps;

import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto;
import ru.citeck.ecos.uiserv.domain.action.service.ActionService;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class ActionArtifactHandler implements WsAwareArtifactHandler<ActionDto> {

    private final ActionService actionService;

    @Override
    public void deployArtifact(@NotNull ActionDto actionModule, @NotNull String workspace) {
        ActionDto action = new ActionDto(actionModule);
        action.setWorkspace(workspace);
        actionService.updateAction(action);
    }

    @Override
    public void deleteArtifact(@NotNull String artifactId, @NotNull String workspace) {
        actionService.deleteAction(IdInWs.create(workspace, artifactId));
    }

    @Override
    public void listenChanges(@NotNull BiConsumer<ActionDto, String> listener) {
        actionService.onActionChanged((before, after) -> {
            String workspace = after.getWorkspace();
            ActionDto action = new ActionDto(after);
            action.setWorkspace("");
            listener.accept(action, workspace);
            return Unit.INSTANCE;
        });
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/action";
    }
}
