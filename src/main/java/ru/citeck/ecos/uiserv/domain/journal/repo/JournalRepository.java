package ru.citeck.ecos.uiserv.domain.journal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Repository
public interface JournalRepository extends JpaRepository<JournalEntity, Long>, JpaSpecificationExecutor<JournalEntity> {

    Optional<JournalEntity> findByExtIdAndWorkspace(String extId, String workspace);

    Set<JournalEntity> findAllByExtIdInAndWorkspace(Set<String> ids, String workspace);

    @Query("SELECT max(j.lastModifiedDate) FROM JournalEntity j")
    Optional<Instant> getLastModifiedTime();
}
