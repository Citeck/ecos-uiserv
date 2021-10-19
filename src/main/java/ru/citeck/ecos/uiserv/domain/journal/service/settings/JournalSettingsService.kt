package ru.citeck.ecos.uiserv.domain.journal.service.settings

import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto

interface JournalSettingsService {

    fun save(settings: JournalSettingsDto): JournalSettingsDto

    fun getById(id: String): JournalSettingsDto?

    fun delete(id: String): Boolean

    fun searchSettings(journalId: String): List<JournalSettingsDto>

    @Deprecated(message = "use searchSettings method instead of this")
    fun getSettings(authority: String?, journalId: String?): List<JournalSettingsDto>

}