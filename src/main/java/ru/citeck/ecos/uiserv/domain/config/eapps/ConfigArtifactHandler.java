package ru.citeck.ecos.uiserv.domain.config.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.uiserv.domain.config.service.ConfigEntityService;
import ru.citeck.ecos.uiserv.domain.config.dto.ConfigDto;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ConfigArtifactHandler implements EcosArtifactHandler<ConfigDto> {

    private final ConfigEntityService service;

    @Override
    public void deployArtifact(@NotNull ConfigDto configDto) {
        service.update(configDto);
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/config";
    }

    @Override
    public void listenChanges(@NotNull Consumer<ConfigDto> consumer) {
    }
}
