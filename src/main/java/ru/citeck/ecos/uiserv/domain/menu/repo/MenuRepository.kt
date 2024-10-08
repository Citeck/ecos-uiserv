package ru.citeck.ecos.uiserv.domain.menu.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface MenuRepository : JpaRepository<MenuEntity, Long>, JpaSpecificationExecutor<MenuEntity> {

    fun findByExtId(extId: String): MenuEntity?

    @Query(
        "SELECT menu " +
            "FROM MenuEntity menu " +
            "JOIN menu.authorities authority " +
            "WHERE lower(authority) = ?1 AND (menu.workspace IS NULL OR menu.workspace = '') " +
            "ORDER BY menu.priority DESC"
    )
    fun findAllByAuthoritiesContains(authority: String): List<MenuEntity>

    @Query(
        "SELECT menu " +
            "FROM MenuEntity menu " +
            "JOIN menu.authorities authority " +
            "WHERE lower(authority) = ?1 AND menu.workspace = ?2 " +
            "ORDER BY menu.priority DESC"
    )
    fun findAllByAuthoritiesContains(authority: String, workspace: String): List<MenuEntity>

    fun deleteByExtId(extId: String)

    @Query("SELECT max(m.lastModifiedDate) FROM MenuEntity m")
    fun getLastModifiedTime(): Instant?

    @Query(value = "SELECT DISTINCT authority FROM ecos_menu_authority", nativeQuery = true)
    fun getAllAuthoritiesWithMenu(): Set<String>
}
