package ru.citeck.ecos.uiserv.domain.dashdoard.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardArtifactHandler implements WsAwareArtifactHandler<DashboardDto> {

    private final DashboardService dashboardService;
    private final WorkspaceService workspaceService;

    @Override
    public void deployArtifact(@NotNull DashboardDto module, @NotNull String workspace) {
        log.info("Dashboard artifact received: {} {}", module.getId(), module.getTypeRef());
        AuthContext.runAsSystemJ(() -> {
            DashboardDto.Builder builder = module.copy().withWorkspace(workspace);
            applyRefs(module, builder, ref ->
                ref.withLocalId(workspaceService.replaceCurrentWsPlaceholderToWsPrefix(ref.getLocalId(), workspace))
            );
            dashboardService.saveDashboard(builder.build());
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
                DashboardDto.Builder builder = after.copy().withWorkspace("");
                applyRefs(after, builder, ref ->
                    ref.withLocalId(workspaceService.replaceWsPrefixToCurrentWsPlaceholder(ref.getLocalId()))
                );
                consumer.accept(builder.build(), workspace);
            }
        });
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/dashboard";
    }

    private void applyRefs(DashboardDto source, DashboardDto.Builder builder, Function<EntityRef, EntityRef> transform) {
        if (EntityRef.isNotEmpty(source.getTypeRef())) {
            builder.withTypeRef(transform.apply(source.getTypeRef()));
        }
        if (EntityRef.isNotEmpty(source.getAppliedToRef())) {
            builder.withAppliedToRef(transform.apply(source.getAppliedToRef()));
        }
    }
}
