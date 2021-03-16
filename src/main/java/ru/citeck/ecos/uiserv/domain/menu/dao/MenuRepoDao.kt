package ru.citeck.ecos.uiserv.domain.menu.dao

import org.springframework.stereotype.Component
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuEntity
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuRepository
import java.time.Instant

@Component
class MenuRepoDao(private val repo: MenuRepository) : MenuDao {

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

    override fun findAll(): List<MenuEntity> {
        return repo.findAll()
    }
}
