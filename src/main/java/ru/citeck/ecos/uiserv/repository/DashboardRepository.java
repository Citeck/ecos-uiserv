package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.DashboardEntity;

import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface DashboardRepository extends JpaRepository<DashboardEntity, Long> {

    @Query("SELECT dashboard FROM DashboardEntity dashboard " +
           "WHERE dashboard.type = ?1 AND dashboard.key = ?2 AND (dashboard.authority = ?3 OR dashboard.authority IS NULL)")
    Optional<DashboardEntity> findForAuthority(String type, String key, String user);

    @Query("SELECT dashboard FROM DashboardEntity dashboard " +
           "WHERE dashboard.type = ?1 AND dashboard.key = ?2 AND dashboard.authority IS NULL")
    Optional<DashboardEntity> findForAll(String type, String key);

    Optional<DashboardEntity> findByTypeAndKeyAndAuthority(String type, String key, String authority);

    Optional<DashboardEntity> findByTypeAndKey(String type, String key);

    Optional<DashboardEntity> findByExtId(String extId);
}
