package ru.citeck.ecos.uiserv.domain.i18n.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.uiserv.domain.i18n.service.I18nService;
import ru.citeck.ecos.uiserv.domain.i18n.dto.I18nDto;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class I18nArtifactHandler implements EcosArtifactHandler<I18nDto> {

    private final I18nService service;

    @Override
    public void deployArtifact(@NotNull I18nDto dto) {
        service.upload(dto);
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/i18n";
    }

    @Override
    public void listenChanges(@NotNull Consumer<I18nDto> consumer) {
    }
}
