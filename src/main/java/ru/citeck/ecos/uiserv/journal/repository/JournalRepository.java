package ru.citeck.ecos.uiserv.journal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.journal.domain.JournalEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Repository
public interface JournalRepository extends JpaRepository<JournalEntity, Long>, JpaSpecificationExecutor<JournalEntity> {

    Optional<JournalEntity> findByExtId(String id);

    Set<JournalEntity> findAllByExtIdIn(Set<String> ids);

    Set<JournalEntity> findAllByTypeRef(String typeRefStr);

    @Query("SELECT max(j.lastModifiedDate) FROM JournalEntity j")
    Optional<Instant> getLastModifiedTime();
}
