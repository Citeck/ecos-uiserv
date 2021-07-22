package ru.citeck.ecos.uiserv.domain.menu.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.NameUtils;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDeployArtifact;
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MenuArtifactHandler implements EcosArtifactHandler<MenuDeployArtifact> {

    private final MenuService menuService;

    @Override
    public void deployArtifact(@NotNull MenuDeployArtifact menuModule) {
        menuService.upload(menuModule);
    }

    @Override
    public void deleteArtifact(@NotNull String s) {
        menuService.deleteByExtId(s);
    }

    @Override
    public void listenChanges(@NotNull Consumer<MenuDeployArtifact> consumer) {

        menuService.addOnChangeListener(menuDto -> {

            MenuDeployArtifact artifact = new MenuDeployArtifact();
            artifact.setFilename(NameUtils.escape(menuDto.getId()) + ".json");
            artifact.setData(Json.getMapper().toBytes(menuDto));
            artifact.setId(menuDto.getId());

            consumer.accept(artifact);
        });
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/menu";
    }
}
