package ru.citeck.ecos.uiserv.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.service.journal.JournalConfigService;
import ru.citeck.ecos.uiserv.service.journal.JournalPrefService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/journalprefs")
@Transactional
public class JournalPrefApi {
    @Autowired
    private JournalPrefService journalPrefService;

    @Autowired
    private JournalConfigService journalConfigService;

    @Autowired
    private FileService fileService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public JsonNode getJournalPrefs(@RequestParam String id) {
        return journalPrefService.getJournalPrefs(id).get().getData();
    }

    //Only for existing prefIds; don't use for creation even if you know the id to assign
    @PutMapping
    public void putJournalPrefs(@RequestParam String id,
                                @RequestBody byte[] bytes) {
        //Verify incoming data, as FileService currently doesn't do that
        try {
            objectMapper.readValue(bytes, JsonNode.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        fileService.deployFileOverride(FileType.JOURNALPREFS, id, null, bytes,
            null /*=don't change metadata*/);
    }

    //Like POST, but accepts id rather than generates it, and cannot be user-targeted
    //Unaccessible from REST API, todo move to JournalPrefService together with most logics residing here :(
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

    //todo since we use http verbs, should make "put" follow correct pattern as well: have id in Path not RequestParam
    @DeleteMapping("/id/{journalViewPrefsId}")
    public void deleteJournalPrefs(@PathVariable String journalViewPrefsId) {
        fileService.deployFileOverride(FileType.JOURNALPREFS, journalViewPrefsId, null, null,
            Collections.emptyMap());
    }

    @PostMapping
    public String postJournalPrefs(@RequestParam String journalId,
                                 @RequestParam(required = false) JournalPrefService.TargetType target,
                                 @ModelAttribute("username") @NonNull String username,
                                 @RequestBody byte[] bytes) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.contains("@")) {
            throw new IllegalArgumentException("Username cannot contain @"); //or we could escape it somehow
        }

        //final JournalConfigService.JournalConfigDownstream journal = journalConfigService.getJournalConfig(journalId)
        //    .orElseThrow(() -> new IllegalArgumentException("Journal config not found: " + journalId));

        //Verify incoming data, as FileService currently doesn't do that
        try {
            objectMapper.readValue(bytes, JsonNode.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return journalPrefService.deployOverride(UUID.randomUUID().toString(), bytes,
            username, target, journalId);
    }

    @GetMapping("/list")
    public List<JournalPrefService.JournalPreferences> getJournalPrefs(@RequestParam String journalId,
                                                                       @ModelAttribute("username") String username,
                                                                       @RequestParam(defaultValue = "true")
                                                                       Boolean includeUserLocal) {
        if (username == null) {
            throw new IllegalArgumentException("Expecting username");
        }
        if (username.equals("")) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.contains("@")) {
            throw new IllegalArgumentException("Username cannot contain @"); //or we could escape it somehow
        }
        return journalPrefService.find(journalId, username, includeUserLocal);
    }
}
