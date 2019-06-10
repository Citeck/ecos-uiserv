package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileMeta;
import ru.citeck.ecos.uiserv.domain.FileVersion;

import java.util.List;
import java.util.Optional;


@SuppressWarnings("unused")
@Repository
public interface FileMetaRepository extends JpaRepository<FileMeta, Long>, JpaSpecificationExecutor<FileMeta> {
   List<FileMeta> findByFile(File file);
   Optional<FileVersion> findOneByFileAndKey(File file, String key);
}
