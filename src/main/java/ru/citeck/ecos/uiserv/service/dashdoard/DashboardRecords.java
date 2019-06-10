package ru.citeck.ecos.uiserv.service.dashdoard;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;
import ru.citeck.ecos.uiserv.domain.EntityDTO;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;
import ru.citeck.ecos.uiserv.service.entity.BaseEntityService;

/**
 * @author Roman Makarskiy
 */
@Component
public class DashboardRecords extends AbstractEntityRecords {

    public static final String ID = "dashboard";

    {
        setId(ID);
    }

    //Its safe, because we know - DashboardEntityService extends Abstract class with <DashboardDTO>
    @SuppressWarnings("unchecked")
    public DashboardRecords(@Qualifier("DashboardEntityService") BaseEntityService entityService) {
        this.entityService = entityService;
    }

    @Override
    protected EntityDTO getEmpty() {
        return new DashboardDTO();
    }
}
