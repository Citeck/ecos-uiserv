package ru.citeck.ecos.uiserv.domain.dashdoard.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardModuleHandler implements EcosModuleHandler<DashboardDto> {

    private final DashboardService dashboardService;

    @Override
    public void deployModule(@NotNull DashboardDto module) {
        log.info("Dashboard module received: " + module.getId() + " " + module.getTypeRef());
        dashboardService.saveDashboard(module);
    }

    @Override
    public void listenChanges(@NotNull Consumer<DashboardDto> consumer) {
        dashboardService.addChangeListener(consumer);
    }

    @Nullable
    @Override
    public ModuleWithMeta<DashboardDto> prepareToDeploy(@NotNull DashboardDto dashboardModule) {
        Optional<DashboardDto> dashboardById = dashboardService.getDashboardById(dashboardModule.getId());
        if (dashboardById.isPresent()) {
            return null;
        }
        return getModuleMeta(dashboardModule);
    }

    @NotNull
    @Override
    public ModuleWithMeta<DashboardDto> getModuleMeta(@NotNull DashboardDto dashboardModule) {
        return new ModuleWithMeta<>(dashboardModule, new ModuleMeta(dashboardModule.getId(), Collections.emptyList()));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/dashboard";
    }
}

