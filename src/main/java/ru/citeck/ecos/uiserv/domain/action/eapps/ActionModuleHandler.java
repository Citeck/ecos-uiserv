package ru.citeck.ecos.uiserv.domain.action.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.uiserv.domain.action.service.ActionService;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionModule;

import java.util.Collections;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ActionModuleHandler implements EcosModuleHandler<ActionModule> {

    private final ActionService actionService;

    @Override
    public void deployModule(@NotNull ActionModule actionModule) {
        actionService.updateAction(actionModule);
    }

    @NotNull
    @Override
    public ModuleWithMeta<ActionModule> getModuleMeta(@NotNull ActionModule module) {
        return new ModuleWithMeta<>(module, new ModuleMeta(module.getId(), Collections.emptyList()));
    }

    @Override
    public void listenChanges(@NotNull Consumer<ActionModule> consumer) {
        actionService.onActionChanged(consumer);
    }

    @Nullable
    @Override
    public ModuleWithMeta<ActionModule> prepareToDeploy(@NotNull ActionModule actionModule) {
        return getModuleMeta(actionModule);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/action";
    }
}

