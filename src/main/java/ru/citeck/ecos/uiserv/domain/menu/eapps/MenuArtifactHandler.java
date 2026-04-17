package ru.citeck.ecos.uiserv.domain.menu.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.NameUtils;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDeployArtifact;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService;

import java.util.function.BiConsumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MenuArtifactHandler implements WsAwareArtifactHandler<MenuDeployArtifact> {

    private final MenuService menuService;

    @Override
    public void deployArtifact(@NotNull MenuDeployArtifact menuModule, @NotNull String workspace) {
        menuService.upload(menuModule, workspace);
    }

    @Override
    public void deleteArtifact(@NotNull String artifactId, @NotNull String workspace) {
        menuService.deleteByExtId(artifactId, workspace);
    }

    @Override
    public void listenChanges(@NotNull BiConsumer<MenuDeployArtifact, String> consumer) {

        menuService.addOnChangeListener((before, after) -> {
            if (after == null) {
                return;
            }
            String workspace = after.getWorkspace() == null ? "" : after.getWorkspace();
            MenuDto stripped = after.copy().withWorkspace("").build();

            MenuDeployArtifact artifact = new MenuDeployArtifact();
            artifact.setFilename(NameUtils.escape(stripped.getId()) + ".json");
            artifact.setData(Json.getMapper().toBytes(stripped));
            artifact.setId(stripped.getId());

            consumer.accept(artifact, workspace);
        });
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/menu";
    }
}
