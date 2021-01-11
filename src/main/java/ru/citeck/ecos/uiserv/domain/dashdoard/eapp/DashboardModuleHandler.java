package ru.citeck.ecos.uiserv.domain.dashdoard.eapp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.uiserv.app.application.constants.AppConstants;
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;

import java.util.Collections;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardModuleHandler implements EcosModuleHandler<DashboardDto> {

    private final DashboardService dashboardService;

    @Override
    public void deployModule(@NotNull DashboardDto module) {
        log.info("Dashboard module received: " + module.getId() + " " + module.getTypeRef());
        SecurityUtils.doAsUser(AppConstants.SYSTEM_ACCOUNT, () ->
            dashboardService.saveDashboard(module)
        );
    }

    @Override
    public void listenChanges(@NotNull Consumer<DashboardDto> consumer) {
        dashboardService.addChangeListener(consumer);
    }

    @Nullable
    @Override
    public ModuleWithMeta<DashboardDto> prepareToDeploy(@NotNull DashboardDto dashboardModule) {
        return getModuleMeta(dashboardModule);
    }

    @NotNull
    @Override
    public ModuleWithMeta<DashboardDto> getModuleMeta(@NotNull DashboardDto dashboardModule) {
        return new ModuleWithMeta<>(dashboardModule, new ModuleMeta(
            dashboardModule.getId(),
            new MLText(),
            Collections.emptyList(),
            Collections.emptyList()
        ));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/dashboard";
    }
}

