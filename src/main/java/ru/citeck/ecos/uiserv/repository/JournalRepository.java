package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.journal.JournalEntity;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

@Repository
public interface JournalRepository extends JpaRepository<JournalEntity, Long>, JpaSpecificationExecutor<JournalEntity> {

    Optional<JournalEntity> findByExtId(String id);

    Set<JournalEntity> findAllByExtIdIn(Set<String> ids);

    Set<JournalEntity> findAllByTypeRef(String typeRefStr);
}
