package ru.citeck.ecos.uiserv.domain.action.eapp;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.uiserv.domain.action.service.ActionService;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto;

import java.util.Collections;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ActionModuleHandler implements EcosModuleHandler<ActionDto> {

    private final ActionService actionService;

    @Override
    public void deployModule(@NotNull ActionDto actionModule) {
        actionService.updateAction(actionModule);
    }

    @NotNull
    @Override
    public ModuleWithMeta<ActionDto> getModuleMeta(@NotNull ActionDto module) {
        return new ModuleWithMeta<>(module, new ModuleMeta(
            module.getId(),
            module.getName(),
            Collections.emptyList(),
            Collections.emptyList()
        ));
    }

    @Override
    public void listenChanges(@NotNull Consumer<ActionDto> consumer) {
        actionService.onActionChanged(consumer);
    }

    @Nullable
    @Override
    public ModuleWithMeta<ActionDto> prepareToDeploy(@NotNull ActionDto actionModule) {
        return getModuleMeta(actionModule);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/action";
    }
}

