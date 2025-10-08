package ru.citeck.ecos.uiserv.domain.form.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormArtifactHandler implements EcosArtifactHandler<EcosFormDef> {

    private final EcosFormService formService;

    @Override
    public void deployArtifact(@NotNull EcosFormDef formModel) {
        log.info("Form artifact received: " + formModel.getId() + " " + formModel.getFormKey());
        formService.save(formModel);
    }

    @Override
    public void listenChanges(@NotNull Consumer<EcosFormDef> consumer) {
        formService.addChangeListener((before, after) -> consumer.accept(after));
    }

    @Override
    public void deleteArtifact(@NotNull String s) {
        formService.delete(IdInWs.create(s));
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/form";
    }
}
