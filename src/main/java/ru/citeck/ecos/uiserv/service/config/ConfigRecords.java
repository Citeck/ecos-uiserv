package ru.citeck.ecos.uiserv.service.config;

import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.uiserv.config.UiServProperties;
import ru.citeck.ecos.uiserv.domain.ConfigDto;
import ru.citeck.ecos.uiserv.domain.EntityDto;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
@Component
public class ConfigRecords extends AbstractEntityRecords<ConfigDto> {

    public static final String ID = "config";

    private UiServProperties properties;

    @Autowired
    public ConfigRecords(ConfigEntityService entityService,
                         UiServProperties properties) {
        setId(ID);
        this.entityService = entityService;
        this.properties = properties;
    }

    @Override
    public RecordsMutResult save(List<ConfigDto> values) {
        RecordsMutResult recordsMutResult = new RecordsMutResult();
        values.forEach(entityDTO -> {
            EntityDto saved;
            String id = entityDTO.getId();

            if (StringUtils.isBlank(id)) {
                throw new IllegalArgumentException("Parameter 'id' is mandatory for config record");
            }

            Optional<ConfigDto> found = entityService.getById(id);
            if (found.isPresent()) {
                saved = entityService.update(entityDTO);
            } else {
                saved = entityService.create(entityDTO);
            }

            RecordMeta recordMeta = new RecordMeta(saved.getId());
            recordsMutResult.addRecord(recordMeta);
        });
        return recordsMutResult;
    }

    @Override
    public List<ConfigDto> getValuesToMutate(List<RecordRef> records) {
        List<ConfigDto> result = new ArrayList<>();
        for (RecordRef recordRef : records) {
            String id = recordRef.getId();
            if (StringUtils.isBlank(id)) {
                result.add(getEmpty());
                continue;
            }

            Optional<ConfigDto> found = entityService.getById(id);
            if (found.isPresent()) {
                result.add(found.get());
            } else {
                ConfigDto dto = new ConfigDto();
                dto.setId(id);
                result.add(dto);
            }
        }
        return result;
    }

    @Override
    public List<ConfigDto> getMetaValues(List<RecordRef> records) {
        List<ConfigDto> result = new ArrayList<>();
        for (RecordRef recordRef : records) {
            String id = recordRef.getId();
            if (StringUtils.isBlank(id)) {
                result.add(getEmpty());
                continue;
            }
            result.add(getConfigDtoById(id));
        }
        return result;
    }

    private ConfigDto getConfigDtoById(String id) {
        Optional<ConfigDto> found = entityService.getById(id);
        return found.orElseGet(() -> getConfigFromProps(id).orElseGet(this::getEmpty));
    }

    private Optional<ConfigDto> getConfigFromProps(String id) {
        return properties.getProperty(id).map(v -> {
            ConfigDto config = new ConfigDto();
            config.setId(id);
            config.setTitle(v.getTitle());
            config.setDescription(v.getDescription());
            config.setValue(TextNode.valueOf(v.getValue()));
            return config;
        });
    }

    @Override
    protected ConfigDto getEmpty() {
        return new ConfigDto();
    }
}
