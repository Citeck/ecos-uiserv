package ru.citeck.ecos.uiserv.service.config;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.uiserv.domain.ConfigDTO;
import ru.citeck.ecos.uiserv.domain.EntityDTO;
import ru.citeck.ecos.uiserv.service.RecordNotFoundException;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;
import ru.citeck.ecos.uiserv.service.entity.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Component
public class ConfigRecords extends AbstractEntityRecords {

    public static final String ID = "config";

    {
        setId(ID);
    }

    //TODO: we want to return a 'config key' as result id?
    @Override
    public RecordsMutResult save(List<EntityDTO> values) {
        RecordsMutResult recordsMutResult = new RecordsMutResult();
        values.forEach(entityDTO -> {
            EntityDTO saved;

            if (StringUtils.isBlank(entityDTO.getId())) {
                saved = entityService.create(entityDTO);
            } else {
                saved = entityService.update(entityDTO);
            }

            RecordMeta recordMeta = new RecordMeta(saved.getKey());
            recordsMutResult.addRecord(recordMeta);
        });
        return recordsMutResult;
    }

    @Override
    public List<EntityDTO> getValuesToMutate(List<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(key ->
                Optional.of(key)
                    .filter(str -> !str.isEmpty())
                    .map(x -> entityService.getByKey(x)
                        .orElseThrow(() -> new RecordNotFoundException("Config with key " + key + " not found!")))
                    .orElseGet(this::getEmpty))
            .collect(Collectors.toList());
    }

    @Override
    public List<EntityDTO> getMetaValues(List<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(key -> Optional.of(key)
                .filter(str -> !str.isEmpty())
                .map(x -> entityService.getByKey(x)
                    .orElseThrow(() -> new RecordNotFoundException("Config with key " + key + " not found!")))
                .orElseGet(this::getEmpty))
            .collect(Collectors.toList());
    }

    //Its safe, because we know - ConfigEntityService extends Abstract class with <ConfigDTO>
    @SuppressWarnings("unchecked")
    public ConfigRecords(@Qualifier("ConfigEntityService") BaseEntityService entityService) {
        this.entityService = entityService;
    }

    @Override
    protected EntityDTO getEmpty() {
        return new ConfigDTO();
    }

}
