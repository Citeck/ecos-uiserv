package ru.citeck.ecos.uiserv.service.form;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.listener.EcosModuleListener;
import ru.citeck.ecos.commons.json.Json;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormModuleListener implements EcosModuleListener<FormModule> {

    private final EcosFormService formService;

    @Override
    public void onModuleDeleted(@NotNull String id) {
        log.info("Form module deleted: " + id);
        formService.delete(id);
    }

    @Override
    public void onModulePublished(FormModule module) {

        log.info("Form module received: " + module.getId() + " " + module.getFormKey());
        EcosFormModel formModel = Json.getMapper().convert(module, EcosFormModel.class);
        formService.save(formModel);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/form";
    }
}

