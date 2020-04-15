package ru.citeck.ecos.uiserv.service.i18n;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class I18nModuleHandler implements EcosModuleHandler<I18nDto> {

    private final I18nService service;

    @Override
    public void deployModule(@NotNull I18nDto dto) {
        service.upload(dto);
    }

    @NotNull
    @Override
    public ModuleWithMeta<I18nDto> getModuleMeta(@NotNull I18nDto dto) {
        return new ModuleWithMeta<>(dto, new ModuleMeta(dto.getId()));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/i18n";
    }

    @Override
    public void listenChanges(@NotNull Consumer<I18nDto> consumer) {
    }

    @Nullable
    @Override
    public ModuleWithMeta<I18nDto> prepareToDeploy(@NotNull I18nDto dto) {
        return getModuleMeta(dto);
    }
}
