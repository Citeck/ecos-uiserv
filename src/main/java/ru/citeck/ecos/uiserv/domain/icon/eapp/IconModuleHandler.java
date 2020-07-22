package ru.citeck.ecos.uiserv.domain.icon.eapp;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.controller.type.binary.BinModule;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.icon.service.IconService;
import ru.citeck.ecos.uiserv.domain.icon.dto.IconDto;

import java.util.Collections;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class IconModuleHandler implements EcosModuleHandler<BinModule> {

    private final IconService iconService;

    @Override
    public void deployModule(@NotNull BinModule iconModule) {
        iconService.save(convertToDto(iconModule));
    }

    private IconDto convertToDto(BinModule module) {

        String extension = FilenameUtils.getExtension(module.getPath());

        switch (extension) {
            case "yml":
            case "yaml":
            case "json":
                String dataString = new String(module.getData());
                return Json.getMapper().read(dataString, IconDto.class);
            default:
                IconDto dto = module.getMeta().getAs(IconDto.class);
                if (dto == null) {
                    dto = new IconDto();
                }
                dto.setData(module.getData());
                ObjectData config = dto.getConfig();
                if (config == null) {
                    dto.setConfig(ObjectData.create());
                    config = dto.getConfig();
                }
                if (config.get("format", "").isEmpty()) {
                    config.set("format", extension);
                }
                if (StringUtils.isBlank(dto.getType())) {
                    dto.setType("img");
                }
                if (StringUtils.isBlank(dto.getId())) {
                    dto.setId(module.getPath());
                }
                return dto;
        }
    }

    @NotNull
    @Override
    public ModuleWithMeta<BinModule> getModuleMeta(@NotNull BinModule module) {
        IconDto dto = convertToDto(module);
        return new ModuleWithMeta<>(module, new ModuleMeta(dto.getId(), Collections.emptyList()));
    }

    @Override
    public void listenChanges(@NotNull Consumer<BinModule> consumer) {
    }

    @Nullable
    @Override
    public ModuleWithMeta<BinModule> prepareToDeploy(@NotNull BinModule iconModule) {
        return getModuleMeta(iconModule);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/icon";
    }
}
