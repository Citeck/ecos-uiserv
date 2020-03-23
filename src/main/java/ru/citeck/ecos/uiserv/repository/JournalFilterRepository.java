package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.citeck.ecos.uiserv.domain.JournalFilter;

public interface JournalFilterRepository extends JpaRepository<JournalFilter, Long> {
    JournalFilter findByExternalId(String externalId);

    int countByUserName(String userName);

    JournalFilter findTopByUserNameOrderByCreationTimeAsc(String userName);

    Integer deleteByExternalId(String externalId);
}
