package ru.citeck.ecos.uiserv.domain.icon.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface IconRepository extends JpaRepository<IconEntity, Long>, JpaSpecificationExecutor<IconEntity> {

    Optional<IconEntity> findByExtId(String extId);

    @Query("SELECT max(m.lastModifiedDate) FROM IconEntity m")
    Optional<Instant> getLastModifiedTime();
}
