package ru.citeck.ecos.uiserv.domain.file.repo;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for the File entity.
 */
@Deprecated
@Repository
public interface FileRepository extends JpaRepository<File, Long>, JpaSpecificationExecutor<File> {

    Optional<File> findByTypeAndFileId(FileType fileType, String fileId);

    @Query("SELECT file FROM File file WHERE file.type=?1")
    List<File> findByType(FileType fileType, PageRequest page);

    @Query("SELECT COUNT(file) FROM File file WHERE file.type=?1")
    int getCountByType(FileType fileType);
}
