package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileVersion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * Spring Data  repository for the MenuConfigVersion entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    // Optional<MenuConfigVersion> findTopByMenuConfigOrderByOrdinalDesc(File menuConfig);

   Optional<FileVersion> findOneByFileAndOrdinal(File file, long ordinal);
   Optional<FileVersion> findTopByFileAndProductVersionIsNotNullOrderByOrdinalDesc(File file);

   List<FileVersion> findAllByFileId(long id);
}
