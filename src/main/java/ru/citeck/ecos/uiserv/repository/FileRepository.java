package ru.citeck.ecos.uiserv.repository;

import ru.citeck.ecos.uiserv.domain.File;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.FileType;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data  repository for the File entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FileRepository extends JpaRepository<File, Long>, JpaSpecificationExecutor<File> {

    Optional<File> findByTypeAndFileId(FileType fileType, String fileId);

    List<File> findByType(FileType fileType);
}
