package ru.citeck.ecos.uiserv.service.dashdoard;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.DashboardDto;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.UUID;

@Component
public class DefaultDashboardsDeployer {

    private DashboardEntityService entityService;

    public DefaultDashboardsDeployer(DashboardEntityService entityService) {
        this.entityService = entityService;
    }

    @PostConstruct
    void init() {
        deployEmptyDashboard(DashboardConstants.TYPE_USER_DASHBOARD, "DEFAULT");
        deployEmptyDashboard(DashboardConstants.TYPE_SITE_DASHBOARD, "DEFAULT");
        deployEmptyDashboard(DashboardConstants.TYPE_CASE_DETAILS, "DEFAULT");
        deployEmptyDashboard(DashboardConstants.TYPE_PROFILE_DETAILS, "DEFAULT");
    }

    private void deployEmptyDashboard(String type, String key) {
        Optional<DashboardDto> existing = entityService.getByKey(type, key, null);
        if (!existing.isPresent()) {
            DashboardDto entity = new DashboardDto();
            entity.setId(type + "-default");
            entity.setKey(key);
            entity.setType(type);
            entityService.create(entity);
        }
    }
}
