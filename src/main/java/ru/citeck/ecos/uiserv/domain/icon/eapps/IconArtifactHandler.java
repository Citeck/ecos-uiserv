package ru.citeck.ecos.uiserv.domain.icon.eapps;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.apps.artifact.controller.type.binary.BinArtifact;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.icon.service.IconService;
import ru.citeck.ecos.uiserv.domain.icon.dto.IconDto;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class IconArtifactHandler implements EcosArtifactHandler<BinArtifact> {

    private final IconService iconService;

    @Override
    public void deployArtifact(@NotNull BinArtifact iconModule) {
        iconService.save(convertToDto(iconModule));
    }

    @Override
    public void deleteArtifact(@NotNull String s) {
        iconService.deleteById(s);
    }

    private IconDto convertToDto(BinArtifact module) {

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

    @Override
    public void listenChanges(@NotNull Consumer<BinArtifact> consumer) {
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/icon";
    }
}
