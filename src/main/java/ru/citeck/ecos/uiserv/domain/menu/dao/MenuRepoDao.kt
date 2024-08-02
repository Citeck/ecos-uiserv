package ru.citeck.ecos.uiserv.domain.menu.dao

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuEntity
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuRepository
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory
import java.time.Instant

@Component
class MenuRepoDao(
    private val repo: MenuRepository,
    private val jpaSearchConverterFactory: JpaSearchConverterFactory
) : MenuDao {

    private lateinit var searchConv: JpaSearchConverter<MenuEntity>

    @PostConstruct
    fun init() {
        searchConv = jpaSearchConverterFactory.createConverter(MenuEntity::class.java).build()
    }

    override fun findByExtId(extId: String): MenuEntity? {
        return repo.findByExtId(extId)
    }

    override fun findAllByAuthoritiesContains(authority: String): List<MenuEntity> {
        return repo.findAllByAuthoritiesContains(authority)
    }

    override fun deleteByExtId(extId: String) {
        repo.deleteByExtId(extId)
    }

    override fun getLastModifiedTime(): Instant {
        return repo.getLastModifiedTime() ?: Instant.EPOCH
    }

    override fun getAllAuthoritiesWithMenu(): Set<String> {
        return repo.getAllAuthoritiesWithMenu()
    }

    override fun delete(entity: MenuEntity) {
        repo.delete(entity)
    }

    override fun save(entity: MenuEntity): MenuEntity {
        return repo.save(entity)
    }

    override fun getCount(predicate: Predicate): Long {
        return searchConv.getCount(repo, predicate)
    }

    override fun findAll(): List<MenuEntity> {
        return repo.findAll()
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<MenuEntity> {
        return searchConv.findAll(repo, predicate, max, skip, sort)
    }
}
