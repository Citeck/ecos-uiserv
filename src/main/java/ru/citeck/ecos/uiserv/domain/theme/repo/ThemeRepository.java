package ru.citeck.ecos.uiserv.domain.theme.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface ThemeRepository extends JpaRepository<ThemeEntity, Long>, JpaSpecificationExecutor<ThemeEntity> {

    Optional<ThemeEntity> findFirstByExtId(String extId);

    @Query("SELECT max(m.lastModifiedDate) FROM ThemeEntity m")
    Optional<Instant> getLastModifiedTime();
}
