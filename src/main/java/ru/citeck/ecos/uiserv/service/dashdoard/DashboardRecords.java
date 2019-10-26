package ru.citeck.ecos.uiserv.service.dashdoard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.DashboardDto;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

/**
 * @author Roman Makarskiy
 */
@Component
public class DashboardRecords extends AbstractEntityRecords<DashboardDto> {

    public static final String ID = "dashboard";

    @Autowired
    public DashboardRecords(DashboardEntityService entityService) {
        setId(ID);
        this.entityService = entityService;
    }

    @Override
    protected DashboardDto getEmpty() {
        return new DashboardDto();
    }
}
