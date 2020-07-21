package ru.citeck.ecos.uiserv.domain.dashdoard.repo;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface DashboardRepository extends JpaRepository<DashboardEntity, Long> {

    @Query("SELECT dashboard FROM DashboardEntity dashboard " +
           "WHERE dashboard.typeRef = ?1 AND dashboard.authority IN ?2 " +
           "ORDER BY dashboard.priority desc")
    List<DashboardEntity> findForAuthorities(String typeRef, List<String> authorities, PageRequest page);

    @Query("SELECT dashboard FROM DashboardEntity dashboard " +
        "WHERE dashboard.typeRef = ?1 AND dashboard.authority IS NULL")
    Optional<DashboardEntity> findByTypeRefForAll(String typeRef);

    Optional<DashboardEntity> findByAuthorityAndTypeRef(String authority, String typeRef);

    Optional<DashboardEntity> findByExtId(String extId);
}
