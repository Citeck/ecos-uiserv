package ru.citeck.ecos.uiserv.domain.journal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.uiserv.domain.file.repo.File;
import ru.citeck.ecos.uiserv.domain.file.repo.FileType;
import ru.citeck.ecos.uiserv.domain.file.repo.FileVersion;
import ru.citeck.ecos.uiserv.domain.file.repo.FileRepository;
import ru.citeck.ecos.uiserv.domain.file.service.FileService;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Deprecated
public class JournalListService {
    @Autowired
    private FileService fileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileRepository fileRepository;

    public Optional<JournalListDownstream> get(String listId, Locale locale) {
        return fileService.loadFile(FileType.JOURNALLIST, listId).flatMap(x -> unmarshalFile(x, locale));
    }

    private Optional<JournalListDownstream> unmarshalFile(File file, Locale locale) {
        return Optional.of(unmarshal(file.getFileVersion().getBytes(), false, locale));
    }

    //need onlyId because some other parts involve querying other services
    private JournalListDownstream unmarshal(byte[] json, boolean onlyId, Locale locale) {
        final JournalList db;
        //When reading older json versions, we can read those into same class but with custom
        //deserializers or field aliases, then write that one to String and then finally read
        //up-to-date JournalClass from that String.
        try {
            db = objectMapper.readValue(json, JournalList.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final JournalListDownstream result = new JournalListDownstream();
        result.setId(db.getId());
        if (!onlyId) {
            if (db.getLocalizedTitle() != null) {
                result.setTitle(db.getLocalizedTitle().values().stream().findFirst().orElse(db.getId()));
            } else {
                result.setTitle(db.getId());
            }
            result.setJournalIds(db.getJournalIds());
        }
        return result;
    }

    @Bean
    public FileService.FileMetadataExtractorInfo journalListFileMetadataExtractor() {
        return new FileService.FileMetadataExtractorInfo(FileType.JOURNALLIST,
            bytes -> unmarshal(bytes, true, null).getId());
    }

    public List<JournalListDownstream> list(Locale locale) {
        final List<File> files = fileRepository.findAll(
            (Root<File> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                From<File, FileVersion> ver = root.join("fileVersion", JoinType.LEFT);
                return criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("type"), FileType.JOURNALLIST),
                    //filter out hidden files
                    criteriaBuilder.isNotNull(ver.get("bytes")));
            });
        return files.stream()
            .map(x -> unmarshalFile(x, locale))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    @Getter
    @Setter
    public static class JournalListDownstream {
        private String id;
        private String title;
        private List<String> journalIds;
    }

    @Getter
    @Setter
    public static class JournalList {
        private String id;
        private Map<String, String> localizedTitle; //Locale id -> title
        private List<String> journalIds;
    }
}
