package ru.citeck.ecos.uiserv.domain.journal.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class JournalArtifactHandler implements EcosArtifactHandler<JournalDto> {

    private final JournalService journalService;

    @Override
    public void deployArtifact(@NotNull JournalDto module) {
        journalService.save(module);
    }

    @Override
    public void listenChanges(@NotNull Consumer<JournalDto> consumer) {
        journalService.onJournalChanged(consumer);
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/journal";
    }
}

