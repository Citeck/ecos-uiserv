package ru.citeck.ecos.uiserv.journal.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.eapps.mapper.OldJournalMapper;
import ru.citeck.ecos.uiserv.journal.eapps.module.OldJournalModule;
import ru.citeck.ecos.uiserv.journal.service.JournalService;

import java.util.Collections;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class OldJournalModuleHandler implements EcosModuleHandler<OldJournalModule> {

    private final JournalService journalService;
    private final OldJournalMapper oldJournalMapper;

    @Override
    public void deployModule(@NotNull OldJournalModule module) {
        JournalDto dto = oldJournalMapper.moduleToDto(module);
        journalService.save(dto);
    }

    @NotNull
    @Override
    public ModuleWithMeta<OldJournalModule> getModuleMeta(@NotNull OldJournalModule module) {
        return new ModuleWithMeta<>(module, new ModuleMeta(module.getId(), Collections.emptyList()));
    }

    @Override
    public void listenChanges(@NotNull Consumer<OldJournalModule> consumer) {
    }

    @Nullable
    @Override
    public ModuleWithMeta<OldJournalModule> prepareToDeploy(@NotNull OldJournalModule module) {
        return getModuleMeta(module);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/old_journal";
    }
}

