package ru.citeck.ecos.uiserv.domain.dashdoard.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.uiserv.app.application.constants.AppConstants;
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardArtifactHandler implements EcosArtifactHandler<DashboardDto> {

    private final DashboardService dashboardService;

    @Override
    public void deployArtifact(@NotNull DashboardDto module) {
        log.info("Dashboard module received: " + module.getId() + " " + module.getTypeRef());
        SecurityUtils.doAsUser(AppConstants.SYSTEM_ACCOUNT, () ->
            dashboardService.saveDashboard(module)
        );
    }

    @Override
    public void listenChanges(@NotNull Consumer<DashboardDto> consumer) {
        dashboardService.addChangeListener(consumer);
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/dashboard";
    }
}
