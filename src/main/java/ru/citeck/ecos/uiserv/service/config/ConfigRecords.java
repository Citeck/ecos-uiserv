package ru.citeck.ecos.uiserv.service.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.ConfigDTO;
import ru.citeck.ecos.uiserv.domain.EntityDTO;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;
import ru.citeck.ecos.uiserv.service.entity.BaseEntityService;

/**
 * @author Roman Makarskiy
 */
@Component
public class ConfigRecords extends AbstractEntityRecords {

    public static final String ID = "config";

    {
        setId(ID);
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
