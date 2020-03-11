package ru.citeck.ecos.uiserv.service.action;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.listener.EcosModuleListener;

@Component
@RequiredArgsConstructor
public class ActionModuleListener implements EcosModuleListener<ActionModule> {

    private final ActionService actionService;

    @Override
    public void onModuleDeleted(@NotNull String s) {
        actionService.deleteAction(s);
    }

    @Override
    public void onModulePublished(ActionModule actionModule) {
        actionService.updateAction(actionModule);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/action";
    }
}

