package ru.citeck.ecos.uiserv.domain.icon.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.citeck.ecos.uiserv.domain.icon.repo.IconEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IconRepository extends JpaRepository<IconEntity, Long> {

    List<IconEntity> findAllByFamilyAndType(String family, String type);

    List<IconEntity> findAllByFamily(String family);

    Optional<IconEntity> findByExtId(String extId);

    @Query("SELECT max(m.lastModifiedDate) FROM IconEntity m")
    Optional<Instant> getLastModifiedTime();
}
