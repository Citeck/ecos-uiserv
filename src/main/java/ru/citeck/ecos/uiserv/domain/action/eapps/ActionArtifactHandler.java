package ru.citeck.ecos.uiserv.domain.action.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.uiserv.domain.action.service.ActionService;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ActionArtifactHandler implements EcosArtifactHandler<ActionDto> {

    private final ActionService actionService;

    @Override
    public void deployArtifact(@NotNull ActionDto actionModule) {
        actionService.updateAction(actionModule);
    }


    @Override
    public void listenChanges(@NotNull Consumer<ActionDto> consumer) {
        actionService.onActionChanged(consumer);
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/action";
    }
}

