package ru.citeck.ecos.uiserv.domain.journalsettings.repo;

import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalSettingsRepository
    extends JpaRepository<JournalSettingsEntity, Long>,
            JpaSpecificationExecutor<JournalSettingsEntity> {

    @Nullable
    JournalSettingsEntity findByExtId(String id);
}
