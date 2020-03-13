package ru.citeck.ecos.uiserv.service.form;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.commons.json.Json;

import java.util.Collections;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormModuleHandler implements EcosModuleHandler<FormModule> {

    private final EcosFormService formService;

    @Override
    public void deployModule(@NotNull FormModule formModule) {
        log.info("Form module received: " + formModule.getId() + " " + formModule.getFormKey());
        EcosFormModel formModel = Json.getMapper().convert(formModule, EcosFormModel.class);
        formService.save(formModel);
    }

    @Override
    public void listenChanges(@NotNull Consumer<FormModule> consumer) {
        formService.addChangeListener(model -> {
            consumer.accept(Json.getMapper().convert(model, FormModule.class));
        });
    }

    @Nullable
    @Override
    public ModuleWithMeta<FormModule> prepareToDeploy(@NotNull FormModule formModule) {
        return getModuleMeta(formModule);
    }

    @NotNull
    @Override
    public ModuleWithMeta<FormModule> getModuleMeta(@NotNull FormModule formModule) {
        return new ModuleWithMeta<>(formModule, new ModuleMeta(formModule.getId(), Collections.emptyList()));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/form";
    }
}

