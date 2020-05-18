package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.MenuEntity;

import java.util.List;
import java.util.Optional;

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
}
