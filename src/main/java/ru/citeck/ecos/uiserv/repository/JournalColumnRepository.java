package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.journal.JournalColumnEntity;
import ru.citeck.ecos.uiserv.domain.journal.JournalEntity;

import java.util.Optional;
import java.util.Set;

@Repository
public interface JournalColumnRepository extends JpaRepository<JournalColumnEntity, Long> {
}
