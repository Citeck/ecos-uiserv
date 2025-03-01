package ru.citeck.ecos.uiserv.domain.menu.service.testutils

import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.menu.dao.MenuDao
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuEntity
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class MenuInMemDao : MenuDao {

    private val data = ConcurrentHashMap<String, MenuEntity>()
    private var lastModified = Instant.EPOCH

    override fun findAllForWorkspace(workspace: String): List<MenuEntity> {
        return data.values.filter {
            it.workspace == workspace
        }
    }

    override fun getCount(predicate: Predicate): Long {
        return data.size.toLong()
    }

    override fun findByExtId(extId: String): MenuEntity? {
        return data[extId]
    }

    override fun findAllByAuthoritiesContains(authority: String, workspace: String): List<MenuEntity> {
        return if (workspace.isEmpty()) {
            data.values.filter { menu ->
                menu.authorities?.map { it.lowercase() }?.contains(authority) ?: false &&
                    menu.workspace == ""
            }
        } else {
            data.values.filter { menu ->
                menu.authorities?.map { it.lowercase() }?.contains(authority) ?: false &&
                    menu.workspace == workspace
            }
        }
    }

    override fun deleteByExtId(extId: String) {
        data.remove(extId)
    }

    override fun getLastModifiedTime(): Instant {
        return lastModified
    }

    override fun getAllAuthoritiesWithMenu(): Set<String> {
        return data.values.flatMap { it.authorities ?: emptyList() }.toSet()
    }

    override fun delete(entity: MenuEntity) {
        data.remove(entity.extId ?: "")
    }

    override fun save(entity: MenuEntity): MenuEntity {
        data[entity.extId ?: ""] = entity
        lastModified = Instant.now()
        return entity
    }

    override fun findAll(): List<MenuEntity> {
        return data.values.toList()
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<MenuEntity> {
        return findAll()
    }
}
