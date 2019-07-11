package ru.citeck.ecos.uiserv.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.ConfigDTO;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.entity.AbstractBaseEntityService;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
@Service
public class ConfigEntityService extends AbstractBaseEntityService<ConfigDTO> {

    public ConfigEntityService(ObjectMapper objectMapper, FileService fileService) {
        super(ConfigDTO.class, FileType.DASHBOARD);
        this.objectMapper = objectMapper;
        this.fileService = fileService;
    }

    @Override
    public ConfigDTO create(ConfigDTO entity) {
        if (StringUtils.isNotBlank(entity.getId()) && getById(entity.getId()).isPresent()) {
            throw new IllegalArgumentException(String.format("Config with id <%s> already exists, use update instead",
                entity.getId()));
        }
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
        return Optional.empty();
    }

    @Override
    public Optional<ConfigDTO> getByKey(String type, String key) {
        return Optional.empty();
    }

    @Override
    public Optional<ConfigDTO> getByKeys(String type, List<String> keys) {
        return Optional.empty();
    }
}
