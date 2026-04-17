package ru.citeck.ecos.uiserv.domain.form.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;

import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormArtifactHandler implements WsAwareArtifactHandler<EcosFormDef> {

    private final EcosFormService formService;

    @Override
    public void deployArtifact(@NotNull EcosFormDef formModel, @NotNull String workspace) {
        log.info("Form artifact received: " + formModel.getId() + " " + formModel.getFormKey());
        formService.save(formModel.copy().withWorkspace(workspace).build());
    }

    @Override
    public void listenChanges(@NotNull BiConsumer<EcosFormDef, String> listener) {
        formService.addChangeListener((before, after) -> {
            String workspace = after.getWorkspace() != null ? after.getWorkspace() : "";
            EcosFormDef stripped = after.copy().withWorkspace("").build();
            listener.accept(stripped, workspace);
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
