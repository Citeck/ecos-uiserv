package ru.citeck.ecos.uiserv.journal.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.service.JournalService;

import java.util.Collections;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class JournalModuleHandler implements EcosModuleHandler<JournalDto> {

    private final JournalService journalService;

    @Override
    public void deployModule(@NotNull JournalDto module) {
        journalService.update(module);
    }

    @NotNull
    @Override
    public ModuleWithMeta<JournalDto> getModuleMeta(@NotNull JournalDto module) {
        return new ModuleWithMeta<>(module, new ModuleMeta(module.getId(), Collections.emptyList()));
    }

    @Override
    public void listenChanges(@NotNull Consumer<JournalDto> consumer) {
        journalService.onJournalChanged(consumer);
    }

    @Nullable
    @Override
    public ModuleWithMeta<JournalDto> prepareToDeploy(@NotNull JournalDto module) {
        return getModuleMeta(module);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/journal";
    }
}

