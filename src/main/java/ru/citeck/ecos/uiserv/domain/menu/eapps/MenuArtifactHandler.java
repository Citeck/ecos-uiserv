package ru.citeck.ecos.uiserv.domain.menu.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDeployModule;
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MenuArtifactHandler implements EcosArtifactHandler<MenuDeployModule> {
    private final MenuService menuService;

    @Override
    public void deployArtifact(@NotNull MenuDeployModule menuModule) {
        menuService.upload(menuModule);
    }

    @Override
    public void listenChanges(@NotNull Consumer<MenuDeployModule> consumer) {
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/menu";
    }
}
