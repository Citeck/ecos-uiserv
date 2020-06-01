package ru.citeck.ecos.uiserv.service.icon;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.service.icon.dto.IconDto;

import java.util.Base64;
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
        String filename = module.getFilename();
        String extension = getExtension(filename);

        switch (extension) {
            case "json":
                return dtoFromJson(module);
            case "png":
                return dtoFromImg(module);
            default:
                throw new IllegalStateException("Unexpected value: " + extension);
        }
    }

    private IconDto dtoFromJson(IconModule module) {
        String dataString = new String(module.getData());
        return Json.getMapper().read(dataString, IconDto.class);
    }

    private IconDto dtoFromImg(IconModule module) {
        byte[] data = module.getData();
        String dataString = Base64.getEncoder().encodeToString(data);

        IconDto dto = new IconDto();

        String filename = module.getFilename();
        dto.setId(filename);
        dto.setType("img");
        dto.setFormat(getExtension(filename));
        dto.setData(dataString);

        return dto;
    }

    private String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    @NotNull
    @Override
    public ModuleWithMeta<IconModule> getModuleMeta(@NotNull IconModule iconModule) {
        return new ModuleWithMeta<>(iconModule, new ModuleMeta(iconModule.getFilename(), Collections.emptyList()));
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
