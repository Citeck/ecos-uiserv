package ru.citeck.ecos.uiserv.service.config;

import com.netflix.discovery.converters.Auto;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.uiserv.domain.ConfigDTO;
import ru.citeck.ecos.uiserv.domain.EntityDTO;
import ru.citeck.ecos.uiserv.service.RecordNotFoundException;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
@Component
public class ConfigRecords extends AbstractEntityRecords<ConfigDTO> {

    public static final String ID = "config";

    @Autowired
    public ConfigRecords(ConfigEntityService entityService) {
        setId(ID);
        this.entityService = entityService;
    }

    @Override
    public RecordsMutResult save(List<ConfigDTO> values) {
        RecordsMutResult recordsMutResult = new RecordsMutResult();
        values.forEach(entityDTO -> {
            EntityDTO saved;
            String id = entityDTO.getId();

            if (StringUtils.isBlank(id)) {
                throw new IllegalArgumentException("Parameter 'id' is mandatory for config record");
            }

            Optional<ConfigDTO> found = entityService.getById(id);
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
    public List<ConfigDTO> getValuesToMutate(List<RecordRef> records) {
        List<ConfigDTO> result = new ArrayList<>();
        for (RecordRef recordRef : records) {
            String id = recordRef.getId();
            if (StringUtils.isBlank(id)) {
                result.add(getEmpty());
                continue;
            }

            Optional<ConfigDTO> found = entityService.getById(id);
            if (found.isPresent()) {
                result.add(found.get());
            } else {
                ConfigDTO dto = new ConfigDTO();
                dto.setId(id);
                result.add(dto);
            }
        }
        return result;
    }

    @Override
    public List<ConfigDTO> getMetaValues(List<RecordRef> records) {
        List<ConfigDTO> result = new ArrayList<>();
        for (RecordRef recordRef : records) {
            String id = recordRef.getId();
            if (StringUtils.isBlank(id)) {
                result.add(getEmpty());
                continue;
            }

            Optional<ConfigDTO> found = entityService.getById(id);
            if (found.isPresent()) {
                result.add(found.get());
            } else {
                throw new RecordNotFoundException(String.format("Entity with id <%s> not found!", id));
            }
        }
        return result;
    }

    @Override
    protected ConfigDTO getEmpty() {
        return new ConfigDTO();
    }
}
