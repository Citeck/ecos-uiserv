package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.listener.EcosModuleListener;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.DashboardDto;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardModuleListener implements EcosModuleListener<DashboardModule> {

    private final DashboardService dashboardService;

    @Override
    public void onModuleDeleted(@NotNull String id) {
        log.info("Dashboard module deleted: " + id);
        dashboardService.removeDashboard(id);
    }

    @Override
    public void onModulePublished(DashboardModule module) {

        log.info("Dashboard module received: " + module.getId() + " " + module.getTypeRef());

        DashboardDto dto = new DashboardDto();
        dto.setPriority(module.getPriority());
        dto.setId(module.getId());
        dto.setConfig(module.getConfig());
        dto.setTypeRef(RecordRef.create("emodel", "type", module.getTypeRef().getId()));
        dto.setAuthority(module.getAuthority());

        dashboardService.saveDashboard(dto);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/dashboard";
    }
}

