package ru.citeck.ecos.uiserv.domain.journal.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class JournalArtifactHandler implements WsAwareArtifactHandler<JournalDef> {

    private final JournalService journalService;

    @Override
    public void deployArtifact(@NotNull JournalDef module, @NotNull String workspace) {
        journalService.save(module.copy().withWorkspace(workspace).build());
    }

    @Override
    public void listenChanges(@NotNull BiConsumer<JournalDef, String> listener) {
        journalService.onJournalChanged((before, after) -> {
            String workspace = after.getWorkspace() != null ? after.getWorkspace() : "";
            JournalDef stripped = after.copy().withWorkspace("").build();
            listener.accept(stripped, workspace);
        });
    }

    @Override
    public void deleteArtifact(@NotNull String artifactId, @NotNull String workspace) {
        journalService.delete(IdInWs.create(workspace, artifactId));
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/journal";
    }
}
