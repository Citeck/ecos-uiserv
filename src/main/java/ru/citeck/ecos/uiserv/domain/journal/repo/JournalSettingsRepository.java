package ru.citeck.ecos.uiserv.domain.journal.repo;

import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalSettingsRepository
    extends JpaRepository<JournalSettingsEntity, Long>,
            JpaSpecificationExecutor<JournalSettingsEntity> {

    @Nullable
    JournalSettingsEntity findByExtId(String id);

    List<JournalSettingsEntity> findAllByAuthorityAndJournalId(String authority, String journalId);

    List<JournalSettingsEntity> findAllByAuthorityInAndJournalId(List<String> authority, String journalId);
}
