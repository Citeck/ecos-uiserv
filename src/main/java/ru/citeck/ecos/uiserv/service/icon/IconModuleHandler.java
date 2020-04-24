package ru.citeck.ecos.uiserv.service.icon;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.uiserv.service.icon.dto.IconDto;

import java.util.Collections;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class IconModuleHandler implements EcosModuleHandler<IconModule> {
    private final IconService iconService;

    @Override
    public void deployModule(@NotNull IconModule iconModule) {
        iconService.save(iconModuleToDto(iconModule));
    }

    private IconDto iconModuleToDto(IconModule module) {
        IconDto dto = new IconDto();

        dto.setId(module.getId());
        dto.setType(module.getType());
        dto.setFormat(module.getFormat());
        dto.setData(module.getData());

        return dto;
    }

    @NotNull
    @Override
    public ModuleWithMeta<IconModule> getModuleMeta(@NotNull IconModule iconModule) {
        return new ModuleWithMeta<>(iconModule, new ModuleMeta(iconModule.getId(), Collections.emptyList()));
    }

    @Override
    public void listenChanges(@NotNull Consumer<IconModule> consumer) {
    }

    @Nullable
    @Override
    public ModuleWithMeta<IconModule> prepareToDeploy(@NotNull IconModule iconModule) {
        return getModuleMeta(iconModule);
    }


    @NotNull
    @Override
    public String getModuleType() {
        return "ui/icon";
    }
}
