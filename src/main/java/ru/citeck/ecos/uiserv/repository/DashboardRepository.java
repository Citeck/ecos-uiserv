package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.DashboardEntity;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface DashboardRepository extends JpaRepository<DashboardEntity, Long> {

    @Query("SELECT dashboard FROM DashboardEntity dashboard " +
           "WHERE dashboard.typeRef = ?1 AND (dashboard.authority IN ?2 OR dashboard.authority IS NULL) " +
           "ORDER BY dashboard.priority desc")
    List<DashboardEntity> findForAuthorities(String typeRef, List<String> authorities, PageRequest page);

    Optional<DashboardEntity> findByTypeRef(String typeRef);

    Optional<DashboardEntity> findByExtId(String extId);
}
