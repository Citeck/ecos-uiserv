package ru.citeck.ecos.uiserv.domain.dashdoard.repo;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface DashboardRepository extends JpaRepository<DashboardEntity, Long>,
    JpaSpecificationExecutor<DashboardEntity> {

    @Query("SELECT dashboard FROM DashboardEntity dashboard " +
           "WHERE dashboard.typeRef = ?1 AND dashboard.authority IN ?2 AND dashboard.scope = ?3 AND dashboard.workspace = ?4 " +
           "ORDER BY dashboard.priority desc")
    List<DashboardEntity> findForAuthorities(String typeRef, List<String> authorities, String scope, String workspace, PageRequest page);

    @Query("SELECT dashboard FROM DashboardEntity dashboard " +
           "WHERE dashboard.appliedToRef = ?1 AND dashboard.authority IN ?2 AND dashboard.scope = ?3 AND dashboard.workspace = ?4 " +
           "ORDER BY dashboard.priority desc")
    List<DashboardEntity> findForRefAndAuthorities(String nodeRef,
                                                   List<String> authorities,
                                                   String scope,
                                                   String workspace,
                                                   PageRequest page);

    @Query("SELECT dashboard FROM DashboardEntity dashboard " +
        "WHERE dashboard.typeRef = ?1 AND dashboard.authority IS NULL AND dashboard.scope = ?2 AND dashboard.workspace = ?3")
    Optional<DashboardEntity> findByTypeRefForAll(String typeRef, String scope, String workspace);

    @Query("SELECT dashboard FROM DashboardEntity dashboard " +
        "WHERE dashboard.appliedToRef = ?1 AND dashboard.authority IS NULL AND dashboard.scope = ?2 AND dashboard.workspace = ?3")
    Optional<DashboardEntity> findByRecordRefForAll(String recordRef, String scope, String workspace);

    Optional<DashboardEntity> findByAuthorityAndTypeRefAndScopeAndWorkspace(
        String authority,
        String typeRef,
        String scope,
        String workspace
    );

    Optional<DashboardEntity> findByAuthorityAndAppliedToRefAndScopeAndWorkspace(
        String authority,
        String recordRef,
        String scope,
        String workspace
    );

    Optional<DashboardEntity> findByExtId(String extId);
}
