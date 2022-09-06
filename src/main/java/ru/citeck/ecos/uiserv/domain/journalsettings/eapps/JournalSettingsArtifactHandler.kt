package ru.citeck.ecos.uiserv.domain.journalsettings.eapps

import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsService
import java.util.function.Consumer

@Component
@RequiredArgsConstructor
class JournalSettingsArtifactHandler(
    private val journalSettingsService: JournalSettingsService
) : EcosArtifactHandler<JournalSettingsDto> {

    override fun deleteArtifact(artifactId: String) {
        journalSettingsService.delete(artifactId)
    }

    override fun deployArtifact(artifact: JournalSettingsDto) {
        journalSettingsService.save(artifact)
    }

    override fun listenChanges(listener: Consumer<JournalSettingsDto>) {
        journalSettingsService.listenChanges { _, after ->
            if (after != null) {
                listener.accept(after)
            }
        }
    }

    override fun getArtifactType(): String {
        return "ui/journal-settings"
    }
}
