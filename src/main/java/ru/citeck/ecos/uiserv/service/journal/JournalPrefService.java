package ru.citeck.ecos.uiserv.service.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.service.file.FileViewCaching;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Deprecated
@Transactional
public class JournalPrefService {
    private final FileViewCaching<JournalPreferences> caching;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileService fileService;

    public JournalPrefService(FileService fileService) {
        this.fileService = fileService;
        this.caching = new FileViewCaching<JournalPreferences>(
            key -> fileService.loadFile(FileType.JOURNALPREFS, key),
            this::unmarshalFile);
    }

    public Optional<JournalPreferences> getJournalPrefs(String journalPrefsId) {
        return caching.get(journalPrefsId);
    }

    //Traditionally, our unmarshallers can return "file is invisible" (as empty Optional)
    private Optional<JournalPreferences> unmarshalFile(File file) {
        return Optional.ofNullable(unmarshal(file.getFileId(), file.getFileVersion().getBytes()));
    }

    private JournalPreferences unmarshal(String fileId, byte[] json) {
        try {
            return new JournalPreferences(fileId, objectMapper.readValue(json, JsonNode.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String composeLookupKey(String username, JournalPrefService.TargetType target, String journalId) {
        final String owner;
        if (target == null) {
            owner = null;
        } else {
            if (target == JournalPrefService.TargetType.USER) {
                owner = username;
            } else
                throw new IllegalArgumentException("Unsupported journal prefs target type: " + target);
        }
        return (owner != null ? owner : "") + "@" + journalId;
    }


    public String deployOverride(String prefsId, byte[] bytes, String username, TargetType target, String journalId) {
        Map<String, String> meta = new HashMap<>();
        meta.put("lookupKey", composeLookupKey(username, target, journalId));
        File prefs = fileService.deployFileOverride(FileType.JOURNALPREFS, prefsId, null, bytes, meta);
        return prefs.getFileId();
    }

    public List<JournalPreferences> find(String journalId, String username, Boolean includeUserLocal) {
        final List<String> cases = new ArrayList<>(2);
        cases.add(composeLookupKey(null, null, journalId));
        if (includeUserLocal) {
            cases.add(composeLookupKey(username, JournalPrefService.TargetType.USER, journalId));
        }
        return fileService.find("lookupKey", cases).stream()
            .map(this::unmarshalFile)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    public void deployJournalPrefs(String journalViewPrefsId,
                                   String journalId,
                                   JsonNode prefs) {
        final byte[] bytes;
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectMapper.writeValue(output, prefs);
            bytes = output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        fileService.deployFileOverride(FileType.JOURNALPREFS, journalViewPrefsId, null, bytes,
            Collections.singletonMap("journalId", journalId));
    }

    public enum TargetType {
        USER /*, GROUP*/
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class JournalPreferences {
        private String fileId;
        private JsonNode data;
    }

    @Getter
    @Setter
    public static class JournalPreferencesDownstream {
        private String id;
        private String journalId;
        private String owner; //user or null
        private JsonNode preferences;
    }

}
