package ru.citeck.ecos.uiserv.domain.journal.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JournalArtifactHandler implements WsAwareArtifactHandler<JournalDef> {

    private final JournalService journalService;
    private final WorkspaceService workspaceService;

    @Override
    public void deployArtifact(@NotNull JournalDef module, @NotNull String workspace) {
        JournalDef.Builder builder = module.copy().withWorkspace(workspace);
        applyRefs(module, builder, ref ->
            ref.withLocalId(workspaceService.replaceCurrentWsPlaceholderToWsPrefix(ref.getLocalId(), workspace))
        );
        journalService.save(builder.build());
    }

    @Override
    public void listenChanges(@NotNull BiConsumer<JournalDef, String> listener) {
        journalService.onJournalChanged((before, after) -> {
            String workspace = after.getWorkspace() != null ? after.getWorkspace() : "";
            JournalDef.Builder builder = after.copy().withWorkspace("");
            applyRefs(after, builder, ref ->
                ref.withLocalId(workspaceService.replaceWsPrefixToCurrentWsPlaceholder(ref.getLocalId()))
            );
            listener.accept(builder.build(), workspace);
        });
    }

    private void applyRefs(JournalDef source, JournalDef.Builder builder, Function<EntityRef, EntityRef> transform) {
        if (EntityRef.isNotEmpty(source.getTypeRef())) {
            builder.withTypeRef(transform.apply(source.getTypeRef()));
        }
        List<EntityRef> actions = source.getActions();
        if (actions != null && !actions.isEmpty()) {
            builder.withActions(actions.stream()
                .map(ref -> EntityRef.isNotEmpty(ref) ? transform.apply(ref) : ref)
                .collect(Collectors.toList()));
        }
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
