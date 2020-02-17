package ru.citeck.ecos.uiserv.service.dashdoard;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.DashboardDto;

import java.util.Optional;
import java.util.UUID;

@Component
public class DefaultDashboardsDeployer {

    private DashboardEntityService entityService;

    private boolean initialized = false;

    public DefaultDashboardsDeployer(DashboardEntityService entityService) {
        this.entityService = entityService;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (initialized) {
            return;
        }
        deployEmptyDashboard(DashboardConstants.TYPE_USER_DASHBOARD, "DEFAULT");
        deployEmptyDashboard(DashboardConstants.TYPE_SITE_DASHBOARD, "DEFAULT");
        deployEmptyDashboard(DashboardConstants.TYPE_CASE_DETAILS, "DEFAULT");
        deployEmptyDashboard(DashboardConstants.TYPE_PROFILE_DETAILS, "DEFAULT");

        initialized = true;
    }

    private void deployEmptyDashboard(String type, String key) {
        Optional<DashboardDto> existing = entityService.getByKey(type, key, null);
        if (!existing.isPresent()) {
            DashboardDto entity = new DashboardDto();
            entity.setId(UUID.randomUUID().toString());
            entity.setKey(key);
            entity.setType(type);
            entityService.create(entity);
        }
    }
}
