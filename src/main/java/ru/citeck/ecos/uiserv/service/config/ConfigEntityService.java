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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Roman Makarskiy
 */
@Service("ConfigEntityService")
public class ConfigEntityService extends AbstractBaseEntityService<ConfigDTO> {

    private static final String KEY = "key";

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
        String id = StringUtils.isNotBlank(entity.getId()) ? entity.getId() : UUID.randomUUID().toString();
        return saveWithId(id, entity);
    }

    @Override
    public ConfigDTO update(ConfigDTO entity) {
        return saveWithId(entity.getId(), entity);
    }

    private ConfigDTO saveWithId(String id, ConfigDTO entity) {
        checkKey(entity);

        ConfigDTO result = new ConfigDTO();

        result.setId(id);
        result.setKey(entity.getKey());
        result.setValue(entity.getValue());
        result.setTitle(entity.getTitle());
        result.setDescription(entity.getDescription());

        writeToFile(result);
        return result;
    }

    private void checkKey(ConfigDTO entity) {
        if (StringUtils.isBlank(entity.getKey())) {
            throw new IllegalArgumentException("'Key' attribute is mandatory for config entity");
        }
    }

    private void writeToFile(ConfigDTO entity) {
        fileService.deployFileOverride(type, entity.getId(), null,
            toJson(entity), Collections.singletonMap(KEY, entity.getKey()));
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
