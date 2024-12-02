package ru.citeck.ecos.uiserv.domain.menu.dao

import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuEntity
import java.time.Instant

interface MenuDao {

    fun findAllForWorkspace(workspace: String): List<MenuEntity>

    fun findByExtId(extId: String): MenuEntity?

    fun findAllByAuthoritiesContains(authority: String, workspace: String): List<MenuEntity>

    fun deleteByExtId(extId: String)

    fun getLastModifiedTime(): Instant

    fun getAllAuthoritiesWithMenu(): Set<String>

    fun delete(entity: MenuEntity)

    fun save(entity: MenuEntity): MenuEntity

    fun findAll(): List<MenuEntity>

    fun getCount(predicate: Predicate): Long

    fun findAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<MenuEntity>
}
