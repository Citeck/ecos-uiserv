package ru.citeck.ecos.uiserv.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.uiserv.domain.ConfigDTO;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.entity.AbstractBaseEntityService;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
@Service("ConfigEntityService")
public class ConfigEntityService extends AbstractBaseEntityService<ConfigDTO> {

    private RecordsService recordsService;

    public ConfigEntityService(@Lazy RecordsService recordsService,
                               ObjectMapper objectMapper, FileService fileService) {
        super(ConfigDTO.class, FileType.DASHBOARD);
        this.recordsService = recordsService;
        this.objectMapper = objectMapper;
        this.fileService = fileService;
    }

    @Override
    public ConfigDTO create(ConfigDTO entity) {
        return save(entity);
    }

    @Override
    public ConfigDTO update(ConfigDTO entity) {
        return save(entity);
    }

    private ConfigDTO save(ConfigDTO entity) {
        checkId(entity);

        ConfigDTO result = new ConfigDTO();

        result.setId(entity.getId());
        result.setValue(entity.getValue());
        result.setTitle(entity.getTitle());
        result.setDescription(entity.getDescription());

        writeToFile(result);
        return result;
    }

    private void checkId(ConfigDTO entity) {
        if (StringUtils.isBlank(entity.getId())) {
            throw new IllegalArgumentException("'Id' attribute is mandatory for config entity");
        }
    }

    private void writeToFile(ConfigDTO entity) {
        fileService.deployFileOverride(type, entity.getId(), null,
            toJson(entity), null);
    }

    @Override
    public Optional<ConfigDTO> getByRecord(RecordRef recordRef) {
        ConfigKey keys = recordsService.getMeta(recordRef, ConfigKey.class);
        return getByKeys(keys.getKeys());
    }

    private static class ConfigKey {
        private final static String ATT_CONFIG_KEY = "_configKey";

        @MetaAtt(ATT_CONFIG_KEY)
        @Getter
        @Setter
        private List<String> keys;
    }
}
