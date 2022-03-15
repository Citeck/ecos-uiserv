package ru.citeck.ecos.uiserv.domain.form.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormArtifactHandler implements EcosArtifactHandler<EcosFormModel> {

    private final EcosFormService formService;

    @Override
    public void deployArtifact(@NotNull EcosFormModel formModel) {
        log.info("Form module received: " + formModel.getId() + " " + formModel.getFormKey());
        formService.save(formModel);
    }

    @Override
    public void listenChanges(@NotNull Consumer<EcosFormModel> consumer) {
        formService.addChangeListener((before, after) -> consumer.accept(after));
    }

    @Override
    public void deleteArtifact(@NotNull String s) {
        formService.delete(s);
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/form";
    }
}

