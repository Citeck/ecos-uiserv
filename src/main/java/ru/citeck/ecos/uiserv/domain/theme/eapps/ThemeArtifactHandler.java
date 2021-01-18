package ru.citeck.ecos.uiserv.domain.theme.eapps;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.theme.dto.ThemeDto;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ThemeArtifactHandler implements EcosArtifactHandler<ThemeArtifactHandler.DeployModule> {

    private final ThemeService themeService;

    @Override
    public void deployArtifact(@NotNull DeployModule module) {

        ThemeDto theme = new ThemeDto();
        theme.setId(module.getId());
        theme.setResources(module.getResources());
        theme.setName(module.getMeta().getName());
        theme.setImages(module.getMeta().getImages());
        theme.setParentRef(module.getMeta().getParentRef());

        themeService.deploy(theme);
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/theme";
    }

    @Override
    public void listenChanges(@NotNull Consumer<DeployModule> consumer) {
        //todo
    }

    @Data
    public static class DeployModule {
        private String id;
        private DeployModuleMeta meta;
        private Map<String, byte[]> resources;
    }

    @Data
    public static class DeployModuleMeta {
        private MLText name;
        private RecordRef parentRef;
        private Map<String, String> images = new HashMap<>();
    }
}
