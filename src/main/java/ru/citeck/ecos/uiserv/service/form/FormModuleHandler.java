package ru.citeck.ecos.uiserv.service.form;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;

import java.util.Collections;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormModuleHandler implements EcosModuleHandler<EcosFormModel> {

    private final EcosFormService formService;

    @Override
    public void deployModule(@NotNull EcosFormModel formModel) {
        log.info("Form module received: " + formModel.getId() + " " + formModel.getFormKey());
        formService.save(formModel);
    }

    @Override
    public void listenChanges(@NotNull Consumer<EcosFormModel> consumer) {
        formService.addChangeListener(consumer);
    }

    @Nullable
    @Override
    public ModuleWithMeta<EcosFormModel> prepareToDeploy(@NotNull EcosFormModel formModule) {
        return getModuleMeta(formModule);
    }

    @NotNull
    @Override
    public ModuleWithMeta<EcosFormModel> getModuleMeta(@NotNull EcosFormModel formModule) {
        return new ModuleWithMeta<>(formModule, new ModuleMeta(formModule.getId(), Collections.emptyList()));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/form";
    }
}

