package ru.citeck.ecos.uiserv.service.dashdoard;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;

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
        Optional<DashboardDTO> existing = entityService.getByKey(type, key);
        if (!existing.isPresent()) {
            DashboardDTO entity = new DashboardDTO();
            entity.setId(UUID.randomUUID().toString());
            entity.setKey(key);
            entity.setType(type);
            entityService.create(entity);
        }
    }
}
