package ru.citeck.ecos.uiserv.service.dashdoard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

/**
 * @author Roman Makarskiy
 */
@Component
public class DashboardRecords extends AbstractEntityRecords<DashboardDTO> {

    public static final String ID = "dashboard";

    @Autowired
    public DashboardRecords(DashboardEntityService entityService) {
        setId(ID);
        this.entityService = entityService;
    }

    @Override
    protected DashboardDTO getEmpty() {
        return new DashboardDTO();
    }
}
