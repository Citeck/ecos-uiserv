package ru.citeck.ecos.uiserv.domain.journal.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class JournalArtifactHandler implements EcosArtifactHandler<JournalDef> {

    private final JournalService journalService;

    @Override
    public void deployArtifact(@NotNull JournalDef module) {
        journalService.save(module);
    }

    @Override
    public void deleteArtifact(@NotNull String s) {
        journalService.delete(IdInWs.create(s));
    }

    @Override
    public void listenChanges(@NotNull Consumer<JournalDef> consumer) {
        journalService.onJournalChanged((before, after) -> consumer.accept(after));
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/journal";
    }
}

