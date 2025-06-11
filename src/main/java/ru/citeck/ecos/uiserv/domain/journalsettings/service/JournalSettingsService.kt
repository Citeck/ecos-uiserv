package ru.citeck.ecos.uiserv.domain.journalsettings.service

import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto

interface JournalSettingsService {

    fun getCount(predicate: Predicate, workspaces: List<String>): Long

    fun findAll(predicate: Predicate, workspaces: List<String>, max: Int, skip: Int, sort: List<SortBy>): List<EntityWithMeta<JournalSettingsDto>>

    fun save(settings: JournalSettingsDto): JournalSettingsDto

    fun getDtoById(id: String): JournalSettingsDto?

    fun getById(id: String): EntityWithMeta<JournalSettingsDto>?

    fun delete(id: String): Boolean

    fun searchSettings(journalId: String): List<EntityWithMeta<JournalSettingsDto>> {
        return searchSettings(journalId, emptyList())
    }

    fun searchSettings(journalId: String, workspaces: List<String>): List<EntityWithMeta<JournalSettingsDto>>

    fun listenChanges(listener: (JournalSettingsDto?, JournalSettingsDto?) -> Unit)
}
