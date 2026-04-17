package ru.citeck.ecos.uiserv.domain.form.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormArtifactHandler implements WsAwareArtifactHandler<EcosFormDef> {

    private final EcosFormService formService;
    private final WorkspaceService workspaceService;

    @Override
    public void deployArtifact(@NotNull EcosFormDef formModel, @NotNull String workspace) {
        log.info("Form artifact received: " + formModel.getId() + " " + formModel.getFormKey());
        EcosFormDef.Builder builder = formModel.copy().withWorkspace(workspace);
        EntityRef typeRef = formModel.getTypeRef();
        if (EntityRef.isNotEmpty(typeRef)) {
            builder.withTypeRef(typeRef.withLocalId(
                workspaceService.replaceCurrentWsPlaceholderToWsPrefix(typeRef.getLocalId(), workspace)
            ));
        }
        formService.save(builder.build());
    }

    @Override
    public void listenChanges(@NotNull BiConsumer<EcosFormDef, String> listener) {
        formService.addChangeListener((before, after) -> {
            String workspace = after.getWorkspace() != null ? after.getWorkspace() : "";
            EcosFormDef.Builder builder = after.copy().withWorkspace("");
            EntityRef typeRef = after.getTypeRef();
            if (EntityRef.isNotEmpty(typeRef)) {
                builder.withTypeRef(typeRef.withLocalId(
                    workspaceService.replaceWsPrefixToCurrentWsPlaceholder(typeRef.getLocalId())
                ));
            }
            listener.accept(builder.build(), workspace);
        });
    }

    @Override
    public void deleteArtifact(@NotNull String artifactId, @NotNull String workspace) {
        formService.delete(IdInWs.create(workspace, artifactId));
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/form";
    }
}
