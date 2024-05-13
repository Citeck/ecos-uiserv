package ru.citeck.ecos.uiserv.domain.journalsettings.repo;

import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalSettingsRepository
    extends JpaRepository<JournalSettingsEntity, Long>,
            JpaSpecificationExecutor<JournalSettingsEntity> {

    @Nullable
    JournalSettingsEntity findByExtId(String id);

    List<JournalSettingsEntity> findAllByAuthorityAndJournalId(String authority, String journalId);

    @Query(
        "SELECT journal_settings " +
            "FROM JournalSettingsEntity journal_settings " +
            "JOIN journal_settings.authorities authority " +
            "WHERE lower(authority) = ?1 " +
                "AND journal_settings.journalId = ?2 " +
            "ORDER BY journal_settings.name ASC"
    )
    List<JournalSettingsEntity> findAllByAuthoritiesInAndJournalId(String authority, String journalId);

    @Query(
        "SELECT journal_settings " +
            "FROM JournalSettingsEntity journal_settings " +
            "JOIN journal_settings.authorities authority " +
            "WHERE lower(authority) = ?1 " +
            "ORDER BY journal_settings.name ASC"
    )
    List<JournalSettingsEntity> findAllByAuthorities(String authority);
}
