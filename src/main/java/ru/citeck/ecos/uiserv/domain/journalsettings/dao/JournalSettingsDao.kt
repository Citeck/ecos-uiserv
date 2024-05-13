package ru.citeck.ecos.uiserv.domain.journalsettings.dao

import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity

interface JournalSettingsDao {

    fun findByExtId(extId: String): JournalSettingsEntity?

    fun findAllByAuthorities(authority: String): List<JournalSettingsEntity>

    fun findAllByAuthoritiesInAndJournalId(authority: String, journalId: String): List<JournalSettingsEntity>

    fun delete(entity: JournalSettingsEntity)

    fun save(entity: JournalSettingsEntity): JournalSettingsEntity

    fun findAll(): List<JournalSettingsEntity>

    fun getCount(predicate: Predicate): Long

    fun findAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<JournalSettingsEntity>
}
