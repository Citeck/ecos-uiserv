package ru.citeck.ecos.uiserv.domain.menu.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDeployModule;
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MenuModuleHandler implements EcosModuleHandler<MenuDeployModule> {
    private final MenuService menuService;

    @Override
    public void deployModule(@NotNull MenuDeployModule menuModule) {
        menuService.upload(menuModule);
    }

    @NotNull
    @Override
    public ModuleWithMeta<MenuDeployModule> getModuleMeta(@NotNull MenuDeployModule menuModule) {
        return new ModuleWithMeta<>(menuModule, new ModuleMeta(menuModule.getId()));
    }

    @Override
    public void listenChanges(@NotNull Consumer<MenuDeployModule> consumer) {
    }

    @Nullable
    @Override
    public ModuleWithMeta<MenuDeployModule> prepareToDeploy(@NotNull MenuDeployModule menuModule) {
        return getModuleMeta(menuModule);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/menu";
    }
}
