package ru.citeck.ecos.uiserv.service.file;

import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.domain.FileVersion;
import ru.citeck.ecos.uiserv.domain.File_;
import ru.citeck.ecos.uiserv.repository.FileRepository;

import javax.persistence.criteria.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Transactional
@Slf4j
public class FileService {
    private static final int MAX_SIZE = 10*1024*1024;

    private volatile Map<FileType, FileMetadataExtractor> fileMetadataExtractors;

    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository, FileStore fileStore) {
        this.fileRepository = fileRepository;
        this.fileStore = fileStore;
    }

    @Autowired
    private void setFileMetadataExtractors(Collection<FileMetadataExtractorInfo> extractors) {
        this.fileMetadataExtractors = extractors.stream()
            .collect(Collectors.toConcurrentMap(
                FileMetadataExtractorInfo::getFileType, FileMetadataExtractorInfo::getExtractor));
    }

    public FileMetadataExtractor getFileMetadataExtractor(FileType fileType) {
        return fileMetadataExtractors.get(fileType);
    }

    public Optional<File> lockFile(FileType fileType, String fileId) {
        return fileStore.lockFile(fileType, fileId);
    }

    public interface FileMetadataExtractor {
        String getFileId(byte[] bytes);
    }

    @Getter
    public static class FileMetadataExtractorInfo {
        private final FileType fileType;
        private final FileMetadataExtractor extractor;

        public FileMetadataExtractorInfo(FileType fileType, @Lazy FileMetadataExtractor extractor) {
            this.fileType = fileType;
            this.extractor = extractor;
        }
    }

    private enum KNOWN_LANGUAGE {EN, RU}

    private final static Map<FileType, String> extensionMap = new ConcurrentHashMap<>();
    static {
        extensionMap.put(FileType.MENU, ".xml");
        extensionMap.put(FileType.JOURNALCFG, ".json");
        extensionMap.put(FileType.JOURNALPREFS, ".json");
    }

    private final FileStore fileStore;

    public File deployStandardFile(FileType fileType, String fileId, String contentType, byte[] bytes, long productVersion) {
        return deployStandardFile(fileType, fileId, contentType, bytes, productVersion, false /*isRevert*/);
    }

    @SuppressWarnings("unchecked")
    private File deployStandardFile(FileType fileType, String fileId, String contentType, byte[] bytes, long productVersion,
                                                 boolean isRevert) {
        final Optional<File> ocfg = fileStore.loadFile(fileType, fileId);
        final long lastOrdinal = ocfg.flatMap(x -> Optional.ofNullable(x.getLatestOrdinal()))
            .orElse(0L);
        final Optional<FileVersion> activeVersion = ocfg.flatMap(cfg -> Optional.ofNullable(cfg.getFileVersion()));
        final boolean activateThisVersion = activeVersion.map(x -> x.getProductVersion() != null)
            .orElse(true);
        return fileStore.saveFile(fileType, fileId, contentType, bytes, Collections.EMPTY_MAP,
            lastOrdinal+1, productVersion, isRevert, !activateThisVersion);
    }

    public void revertFileOverrides(FileType fileType, String fileId) {
        final Optional<FileVersion> last = fileStore.getFileLastStandardVersion(fileType, fileId);
        //No standard version deployed to revert to?
        if (!last.isPresent()) {
            //so we simply hide overrides, and menu becomes hidden
            deployFileOverride(fileType, fileId, null /*content-type*/, null /*xml*/, null /*=retain metadata*/);
        } else {
            //prerequisite for activating standard version on deploy
            fileStore.clearFileCurrentVersionRef(fileType, fileId);
            deployStandardFile(fileType, fileId, null /*content-type*/, last.get().getBytes(), last.get().getProductVersion(),
                true /*isRevert*/);
        }
    }

    public File deployFileOverride(FileType fileType, String fileId, String contentType, byte[] bytes,
                                   Map<String, String> metadata) {
        //if we make use of JPA's optimistic locking, maybe with our field file.ordinal,
        //we could todo remove this pessimistic lock
        lockFile(fileType, fileId); //does nothing if the file is not yet there! but two attempts to deploy same new file will fail anyway

        final Optional<File> ocfg = fileStore.loadFile(fileType, fileId);
        final long lastOrdinal = ocfg.flatMap(x -> Optional.ofNullable(x.getLatestOrdinal()))
            .orElse(0L);

        return fileStore.saveFile(fileType, fileId, contentType, bytes, metadata,
            lastOrdinal+1, null /*productVersion*/, false/*isRevert*/, false);
    }

    public Optional<File> loadFile(FileType fileType, String fileId) {
        return fileStore.loadFile(fileType, fileId);
    }

    public Optional<File> loadFile(long fileId) {
        return fileStore.loadFile(fileId);
    }

    private KNOWN_LANGUAGE getKnownLanguage(String fileName) {
        return Stream.of(KNOWN_LANGUAGE.values())
            .filter(tag -> fileName.toLowerCase().endsWith(tag.name().toLowerCase() + ".properties"))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("Cannot infer known language from filename: " + fileName));
    }

    public FileBundle readBundleFromZip(byte[] data, FileType fileType) {
        final String fileNameExtension = extensionMap.get(fileType);
        final Map<String, byte[]> translations = new HashMap<>();
        final List<byte[]> mains = new ArrayList<>();
        try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry zipEntry = zipInput.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.getSize() > MAX_SIZE)
                    throw new IllegalArgumentException("Maximum accepted zip size is " + MAX_SIZE);
                final byte[] decompressed;
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    ByteStreams.copy(zipInput, output);
                    decompressed = output.toByteArray();
                }
                //We skip subdirs partly because we don't want to deal with "__MACOSX/*"
                if (!zipEntry.getName().contains("/")) {
                    if (zipEntry.getName().toLowerCase().endsWith(".properties")) {
                        translations.put(getKnownLanguage(zipEntry.getName()).name().toLowerCase(),
                            decompressed);
                    } else if (zipEntry.getName().toLowerCase().endsWith(fileNameExtension)) {
                        mains.add(decompressed);
                    } else {
                        log.warn("Unrecognized entry in ZIP: " + zipEntry.getName());
                    }
                } else {
                    log.warn("Skipping subdirectory related entry in ZIP: " + zipEntry.getName());
                }

                zipEntry = zipInput.getNextEntry();
            }
            zipInput.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (mains.size() == 0)
            throw new IllegalArgumentException("No " + fileNameExtension + " file found in the ZIP");
        if (mains.size() > 1)
            throw new IllegalArgumentException("Multiple " + fileNameExtension + " files found in the ZIP");
        final byte[] fileBytes = mains.iterator().next();
        return new FileBundle(fileBytes, Collections.unmodifiableMap(translations));
    }

    public List<File> findByType(FileType type) {
        return fileRepository.findByType(type);
    }

    //If at some moment we'll really need to find by multiple keys, we'll probably better off by
    //making metadata a document and storing it in some search engine, than using DB-based search
    public List<File> find(String metaKey, List<String> metaValues) {
        if (metaValues.size() == 0) {
            return Collections.emptyList();
        }
        final List<File> asses = fileRepository.findAll(
            (Root<File> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                MapJoin<File, String, String> fileMeta = root.join(File_.fileMeta, JoinType.INNER);
                return criteriaBuilder.and(
                    criteriaBuilder.equal(fileMeta.key(), metaKey),
                    fileMeta.value().in(metaValues.toArray()));
            });
        return asses.stream()
            //we must not return hidden files! i.e. ones deployed as empty bytes[].
            //we could skip this check by just stripping metadata when hiding a file, but we want the metadata to be
            //  preserved so that later we'll be able to upload another version of a file (not changing metadata).
            .filter(file -> file.getFileVersion().getBytes() != null)
            .distinct()
            .collect(Collectors.toList());
    }

    public void delete(FileType fileType, String fileId) {
        fileStore.deleteFile(fileType, fileId);
    }
}
