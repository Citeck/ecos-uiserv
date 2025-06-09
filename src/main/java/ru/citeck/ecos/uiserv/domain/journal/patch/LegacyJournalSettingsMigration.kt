package ru.citeck.ecos.uiserv.domain.journal.patch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthUser
import ru.citeck.ecos.uiserv.domain.file.repo.File
import ru.citeck.ecos.uiserv.domain.file.repo.FileType
import ru.citeck.ecos.uiserv.domain.file.service.FileService
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.service.JournalSettingsService
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosLocalPatch
import java.io.IOException
import java.util.*

@Component
@Suppress("DEPRECATION")
@EcosLocalPatch("legacy-journal-settings-migration", "2025-06-09T00:00:00Z")
class LegacyJournalSettingsMigration(
    private val fileService: FileService,
    private val objectMapper: ObjectMapper,
    private val journalSettingsService: JournalSettingsService
) : Function0<Any?> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun invoke(): Any? {
        val files = fileService.findByType(FileType.JOURNALPREFS, 1_000_000, 0)
        val status = MigrationStatus(files.size)
        log.info { "Found ${status.totalCount} configs to process" }

        for (file in files) {
            val settingsDto: JournalSettingsDto
            try {
                val settingsOrNull = toSettingsDto(file)
                if (settingsOrNull == null) {
                    log.info { "Empty config: " + file.fileId }
                    status.emptyConfigsCount++
                    continue
                }
                settingsDto = settingsOrNull
            } catch (e: Throwable) {
                log.warn {
                    "Failed to read: " + file.fileId + ". Error: " + e::class.simpleName + " - " + e.message
                }
                status.failedToRead++
                continue
            }
            val existingSettings = journalSettingsService.getById(settingsDto.id)
            if (existingSettings == null) {
                journalSettingsService.save(settingsDto)
            } else {
                status.alreadyExists++
            }
            status.processed++
        }

        return status
    }

    private fun toSettingsDto(prefFile: File): JournalSettingsDto? {

        val optPref = unmarshalFile(prefFile)
        if (!optPref.isPresent) {
            return null
        }
        val lookupKey = prefFile.fileMeta?.get("lookupKey") ?: ""
        if (lookupKey.isBlank()) {
            return null
        }
        val userAndJournal = lookupKey.split("@")
        if (userAndJournal.size < 2) {
            return null
        }
        val user = userAndJournal[0]
        val journal = userAndJournal[1]
        if (journal.isBlank()) {
            return null
        }

        val pref = optPref.get()

        val prefSettings = ObjectData.create(pref.data)
        val settings = JournalSettingsDto.create()
            .withId(pref.fileId)
            .withName(Json.mapper.read(prefSettings["title"].asText(), MLText::class.java))
            .withJournalId(journal)
            .withSettings(prefSettings)

        if (user.isNotBlank()) {
            settings.withAuthorities(listOf(user))
                .withCreator(user)
        } else {
            settings.withCreator(AuthUser.SYSTEM)
        }

        return settings.build()
    }

    private fun unmarshalFile(file: File): Optional<JournalPreferences> {
        return Optional.of(unmarshal(file.fileId, file.fileVersion.bytes))
    }

    private fun unmarshal(fileId: String, json: ByteArray): JournalPreferences {
        try {
            return JournalPreferences(fileId, objectMapper.readValue(json, JsonNode::class.java))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    class JournalPreferences(
        var fileId: String,
        var data: JsonNode? = null
    )

    class MigrationStatus(
        val totalCount: Int,
        var alreadyExists: Int = 0,
        var failedToRead: Int = 0,
        var emptyConfigsCount: Int = 0,
        var processed: Int = 0
    )
}
