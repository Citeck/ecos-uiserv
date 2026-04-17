package ru.citeck.ecos.uiserv.domain.dashdoard.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;

import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardArtifactHandler implements WsAwareArtifactHandler<DashboardDto> {

    private final DashboardService dashboardService;

    @Override
    public void deployArtifact(@NotNull DashboardDto module, @NotNull String workspace) {
        log.info("Dashboard artifact received: {} {}", module.getId(), module.getTypeRef());
        AuthContext.runAsSystemJ(() -> {
            dashboardService.saveDashboard(module.copy().withWorkspace(workspace).build());
        });
    }

    @Override
    public void deleteArtifact(@NotNull String artifactId, @NotNull String workspace) {
        dashboardService.removeDashboard(artifactId, workspace);
    }

    @Override
    public void listenChanges(@NotNull BiConsumer<DashboardDto, String> consumer) {
        dashboardService.addChangeListener((before, after) -> {
            if (after != null) {
                String workspace = after.getWorkspace();
                DashboardDto stripped = after.copy().withWorkspace("").build();
                consumer.accept(stripped, workspace);
            }
        });
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/dashboard";
    }
}
