package ru.citeck.ecos.uiserv.domain.menu.dao

import ru.citeck.ecos.uiserv.domain.menu.repo.MenuEntity
import java.time.Instant

interface MenuDao {

    fun findByExtId(extId: String): MenuEntity?

    fun findAllByAuthoritiesContains(authority: String): List<MenuEntity>

    fun deleteByExtId(extId: String)

    fun getLastModifiedTime(): Instant

    fun getAllAuthoritiesWithMenu(): Set<String>

    fun delete(entity: MenuEntity)

    fun save(entity: MenuEntity): MenuEntity

    fun findAll(): List<MenuEntity>
}
