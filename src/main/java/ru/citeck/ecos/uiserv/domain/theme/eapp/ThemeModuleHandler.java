package ru.citeck.ecos.uiserv.domain.theme.eapp;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.theme.dto.ThemeDto;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ThemeModuleHandler implements EcosModuleHandler<ThemeModuleHandler.DeployModule> {

    private final ThemeService themeService;

    @Override
    public void deployModule(@NotNull DeployModule module) {

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
    public ModuleWithMeta<DeployModule> getModuleMeta(@NotNull DeployModule module) {
        return new ModuleWithMeta<>(module, new ModuleMeta(module.id));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/theme";
    }

    @Override
    public void listenChanges(@NotNull Consumer<DeployModule> consumer) {
        //todo
    }

    @Nullable
    @Override
    public ModuleWithMeta<DeployModule> prepareToDeploy(@NotNull DeployModule module) {
        return getModuleMeta(module);
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
