package ru.citeck.ecos.uiserv.service.dashdoard;

import org.springframework.stereotype.Component;

@Component
public class DefaultDashboardsDeployer {

    private DashboardService dashboardService;

    private boolean initialized = false;

    public DefaultDashboardsDeployer(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
}
