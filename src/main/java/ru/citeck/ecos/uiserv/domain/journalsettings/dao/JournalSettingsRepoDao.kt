package ru.citeck.ecos.uiserv.domain.journalsettings.dao

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsRepository
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory

@Component
class JournalSettingsRepoDao(
    private val repo: JournalSettingsRepository,
    private val jpaSearchConverterFactory: JpaSearchConverterFactory
) : JournalSettingsDao {

    private lateinit var searchConv: JpaSearchConverter<JournalSettingsEntity>

    @PostConstruct
    fun init() {
        searchConv = jpaSearchConverterFactory.createConverter(JournalSettingsEntity::class.java).build()
    }

    override fun findByExtId(extId: String): JournalSettingsEntity? {
        return repo.findByExtId(extId)
    }

    override fun findAllByAuthorities(authority: String): List<JournalSettingsEntity> {
        return repo.findAllByAuthorities(authority)
    }

    override fun findAllByAuthoritiesInAndJournalId(authority: String, journalId: String): List<JournalSettingsEntity> {
        return repo.findAllByAuthoritiesInAndJournalId(authority, journalId)
    }

    override fun delete(entity: JournalSettingsEntity) {
        repo.delete(entity)
    }

    override fun save(entity: JournalSettingsEntity): JournalSettingsEntity {
        return repo.save(entity)
    }

    override fun getCount(predicate: Predicate): Long {
        return searchConv.getCount(repo, predicate)
    }

    override fun findAll(): List<JournalSettingsEntity> {
        return repo.findAll()
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<JournalSettingsEntity> {
        return searchConv.findAll(repo, predicate, max, skip, sort)
    }
}
