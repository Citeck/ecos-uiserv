package ru.citeck.ecos.uiserv.domain.file.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.file.repo.File;
import ru.citeck.ecos.uiserv.domain.file.repo.FileType;
import ru.citeck.ecos.uiserv.domain.file.repo.FileVersion;
import ru.citeck.ecos.uiserv.domain.translation.repo.Translated;
import ru.citeck.ecos.uiserv.domain.file.repo.FileRepository;
import ru.citeck.ecos.uiserv.domain.file.repo.FileVersionRepository;
import ru.citeck.ecos.uiserv.domain.translation.repo.TranslatedRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import javax.transaction.Transactional;
import java.util.Map;
import java.util.Optional;

@Deprecated
@Component
public class FileStore {

    @Autowired
    private FileRepository repository;

    @Autowired
    private FileVersionRepository versionRepository;

    @Autowired
    private TranslatedRepository crutches;

    @Autowired
    private HazelcastInstance cache;

    @Autowired
    private EntityManager entityManager;

    private final static String NOT_FOUND = "";

    public Optional<File> loadFile(FileType fileType, String fileId) {
        final IMap<String, String> map = cache.getMap("file/" + fileType);
        final String x = map.get(fileId);
        final Optional<Long> v;
        if (x != null) {
            v = x.equals(NOT_FOUND) ? Optional.empty() : Optional.of(Long.parseLong(x));
        } else {
            v = repository.findByTypeAndFileId(fileType, fileId).map(cfg -> cfg.getId());
            map.put(fileId, v.map(String::valueOf).orElse(NOT_FOUND));
        }
        final Optional<File> file = v.flatMap(repository::findById);
        //todo this requires loading bytes[] to determine if the file is considered hidden or not;
        // need some other flag instead and keep bytes[] lazy-loaded
        return file.map(File::getFileVersion).flatMap(ver -> Optional.ofNullable(ver.getBytes()))
            .flatMap(bytes -> file);
    }

    public Optional<File> loadFile(long fileId) {
        return repository.findById(fileId);
    }

    public File saveFile(FileType fileType, String fileId, String contentType, byte[] bytes,
                         Map<String, String> meta, long ordinal, Long productVersion,
                         boolean isRevert, boolean skipThisVersion) {
        final IMap<String, String> map = cache.getMap("file/" + fileType);
        map.remove(fileId);

        final FileVersion newVersion = new FileVersion();
        newVersion.setTranslated(crutches.save(new Translated()));
        newVersion.setBytes(bytes);
        newVersion.setProductVersion(productVersion);
        newVersion.setIsRevert(isRevert);
        newVersion.setOrdinal(ordinal); //also serves as optimistic lock
        newVersion.setContentType(contentType);
        if (isRevert && productVersion == null)
            throw new IllegalArgumentException("Can only revert to a standard menu");

        File cfg = repository.findByTypeAndFileId(fileType, fileId)
            .orElseGet(File::new);
        cfg.setFileId(fileId);
        cfg.setLatestOrdinal(ordinal);
        cfg.setType(fileType);
        if (cfg.getFileMeta() == null) {
            cfg.setFileMeta(meta);
        }
        cfg = repository.save(cfg);

        if (!skipThisVersion) {
            cfg.setFileVersion(newVersion);
        } else {
            //then we persist it ourselves
            versionRepository.save(newVersion);
        }
        newVersion.setFile(cfg);
        return repository.save(cfg);
    }

    public Optional<FileVersion> getLastFileVersion(FileType fileType, String fileId) {
        return repository.findByTypeAndFileId(fileType, fileId).flatMap(cfg ->
            versionRepository.findOneByFileAndOrdinal(cfg, cfg.getLatestOrdinal()));
    }

    public void clearFileCurrentVersionRef(FileType fileType, String fileId) {
        final File cfg = repository.findByTypeAndFileId(fileType, fileId)
            .orElseThrow(() -> new RuntimeException("File should exist: " + fileId));
        cfg.setFileVersion(null);
        repository.save(cfg);
    }

    public Optional<FileVersion> getFileLastStandardVersion(FileType fileType, String fileId) {
        return repository.findByTypeAndFileId(fileType, fileId).flatMap(
            file -> versionRepository.findTopByFileAndProductVersionIsNotNullOrderByOrdinalDesc(file));
    }

    public Optional<File> lockFile(FileType fileType, String fileId) {
        final Optional<File> file = loadFile(fileType, fileId); //possibly uses cache
        //re-query by known PK, this time placing the lock
        return file.map(File::getId)
            .flatMap(id -> Optional.ofNullable(
                entityManager.find(File.class, id, LockModeType.PESSIMISTIC_WRITE)));
    }

    @Transactional
    public void deleteFile(FileType fileType, String fileId) {

        Optional<File> file = repository.findByTypeAndFileId(fileType, fileId);

        file.ifPresent(f -> {
            versionRepository.deleteAll(versionRepository.findAllByFileId(f.getId()));
            repository.delete(f);
        });
    }
}
