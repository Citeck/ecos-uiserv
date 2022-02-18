package ru.citeck.ecos.uiserv.domain.menu.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MenuRepository extends JpaRepository<MenuEntity, Long> {
    Optional<MenuEntity> findByExtId(String extId);

    @Query("SELECT menu " +
        "FROM MenuEntity menu " +
        "JOIN menu.authorities authority " +
        "WHERE authority = ?1 " +
        "ORDER BY menu.priority DESC")
    List<MenuEntity> findAllByAuthoritiesContains(String authority);

    void deleteByExtId(String extId);

    @Query("SELECT max(m.lastModifiedDate) FROM MenuEntity m")
    Optional<Instant> getLastModifiedTime();

    @Query(
        value = "SELECT DISTINCT authority FROM ecos_menu_authority",
        nativeQuery = true
    )
    Set<String> getAllAuthoritiesWithMenu();
}