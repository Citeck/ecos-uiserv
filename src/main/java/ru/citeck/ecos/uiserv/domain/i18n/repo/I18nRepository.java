package ru.citeck.ecos.uiserv.domain.i18n.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface I18nRepository extends JpaRepository<I18nEntity, Long>, JpaSpecificationExecutor<I18nEntity> {

    Optional<I18nEntity> findByTenantAndExtId(String tenant, String extId);

    @Query("SELECT max(j.lastModifiedDate) FROM I18nEntity j")
    Optional<Instant> getLastModifiedTime();
}
