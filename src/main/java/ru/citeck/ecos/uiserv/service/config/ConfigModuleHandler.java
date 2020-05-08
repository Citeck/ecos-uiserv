package ru.citeck.ecos.uiserv.service.config;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.uiserv.service.config.dto.ConfigDto;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ConfigModuleHandler implements EcosModuleHandler<ConfigDto> {

    private final ConfigEntityService service;

    @Override
    public void deployModule(@NotNull ConfigDto configDto) {
        service.update(configDto);
    }

    @NotNull
    @Override
    public ModuleWithMeta<ConfigDto> getModuleMeta(@NotNull ConfigDto configDto) {
        return new ModuleWithMeta<>(configDto, new ModuleMeta(configDto.getId()));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/config";
    }

    @Override
    public void listenChanges(@NotNull Consumer<ConfigDto> consumer) {
    }

    @Nullable
    @Override
    public ModuleWithMeta<ConfigDto> prepareToDeploy(@NotNull ConfigDto configDto) {
        return getModuleMeta(configDto);
    }
}
