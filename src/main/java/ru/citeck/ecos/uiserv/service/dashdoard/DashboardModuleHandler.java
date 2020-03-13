package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.ModuleRef;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.DashboardDto;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardModuleHandler implements EcosModuleHandler<DashboardModule> {

    private final DashboardService dashboardService;

    @Override
    public void deployModule(@NotNull DashboardModule module) {

        log.info("Dashboard module received: " + module.getId() + " " + module.getTypeRef());

        DashboardDto dto = new DashboardDto();
        dto.setPriority(module.getPriority());
        dto.setId(module.getId());
        dto.setConfig(module.getConfig());
        dto.setTypeRef(RecordRef.create("emodel", "type", module.getTypeRef().getId()));
        dto.setAuthority(module.getAuthority());

        dashboardService.saveDashboard(dto);
    }

    @Override
    public void listenChanges(@NotNull Consumer<DashboardModule> consumer) {

        dashboardService.addChangeListener(dto -> {

            DashboardModule module = new DashboardModule();

            module.setPriority(dto.getPriority());
            module.setId(dto.getId());
            module.setConfig(dto.getConfig());

            RecordRef typeRef = dto.getTypeRef();
            if (typeRef != null) {
                module.setTypeRef(ModuleRef.create("model/type", dto.getTypeRef().getId()));
            }
            module.setAuthority(dto.getAuthority());

            consumer.accept(module);
        });
    }

    @Nullable
    @Override
    public ModuleWithMeta<DashboardModule> prepareToDeploy(@NotNull DashboardModule dashboardModule) {
        Optional<DashboardDto> dashboardById = dashboardService.getDashboardById(dashboardModule.getId());
        if (dashboardById.isPresent()) {
            return null;
        }
        return getModuleMeta(dashboardModule);
    }

    @NotNull
    @Override
    public ModuleWithMeta<DashboardModule> getModuleMeta(@NotNull DashboardModule dashboardModule) {
        return new ModuleWithMeta<>(dashboardModule, new ModuleMeta(dashboardModule.getId(), Collections.emptyList()));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/dashboard";
    }
}

