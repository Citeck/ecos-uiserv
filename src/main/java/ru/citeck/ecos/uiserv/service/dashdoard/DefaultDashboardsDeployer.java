package ru.citeck.ecos.uiserv.service.dashdoard;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.OldDashboardDto;

import java.util.Optional;

@Component
public class DefaultDashboardsDeployer {

    private DashboardService dashboardService;

    private boolean initialized = false;

    public DefaultDashboardsDeployer(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

  /*  @EventListener
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
        Optional<OldDashboardDto> existing = dashboardService.getDashboard(type, key, null);
        if (!existing.isPresent()) {
            OldDashboardDto entity = new OldDashboardDto();
            entity.setId(type + "-default");
            entity.setKey(key);
            entity.setType(type);
            dashboardService.saveDashboard(entity);
        }
    }*/
}
