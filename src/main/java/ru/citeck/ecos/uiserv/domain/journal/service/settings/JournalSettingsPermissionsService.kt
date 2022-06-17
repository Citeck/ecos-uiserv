package ru.citeck.ecos.uiserv.domain.journal.service.settings

import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsEntity

interface JournalSettingsPermissionsService {

    fun canRead(dto: JournalSettingsDto): Boolean

    fun canRead(entity: JournalSettingsEntity): Boolean

    fun canWrite(dto: JournalSettingsDto): Boolean

    fun canWrite(entity: JournalSettingsEntity): Boolean

    fun canWriteNew(dto: JournalSettingsDto): Boolean
}
