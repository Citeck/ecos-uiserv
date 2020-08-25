package ru.citeck.ecos.uiserv.domain.journal.api.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.uiserv.domain.file.repo.FileType;
import ru.citeck.ecos.uiserv.domain.file.service.FileService;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalPrefService;
import ru.citeck.ecos.uiserv.app.web.exception.JournalPrefsNotFoundException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/journalprefs")
@RequiredArgsConstructor
public class JournalPrefApi {

    private final JournalPrefService journalPrefService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    /**
     * Now need to use GET method from v2 package.
     */
    @Deprecated
    @GetMapping
    public JsonNode getJournalPrefs(@RequestParam String id) {
        Optional<JournalPrefService.JournalPreferences> optionalJournalPrefs = journalPrefService.getJournalPrefs(id);
        return optionalJournalPrefs.orElseThrow(() -> new JournalPrefsNotFoundException(id)).getData();
    }

    /**
     * Now need to use PUT method from v2 package.
     */
    @Deprecated
    @PutMapping
    public void putJournalPrefs(@RequestParam String id,
                                @RequestBody byte[] bytes) {
        validateBody(bytes);
        fileService.deployFileOverride(FileType.JOURNALPREFS, id, null, bytes);
    }

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
        validateUsername(username);
        validateBody(bytes);
        return journalPrefService.deployOverride(UUID.randomUUID().toString(), bytes,
            username, target, journalId);
    }

    @GetMapping("/list")
    public List<JournalPrefService.JournalPreferences> getJournalPrefs(@RequestParam String journalId,
                                                                       @ModelAttribute("username") @NonNull
                                                                           String username,
                                                                       @RequestParam(defaultValue = "true")
                                                                           Boolean includeUserLocal) {
        validateUsername(username);
        return journalPrefService.find(journalId, username, includeUserLocal);
    }

    private void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.contains("people@")) {
            throw new IllegalArgumentException("Username cannot contain people@");
        }
    }

    private void validateBody(byte[] body) {
        try {
            objectMapper.readValue(body, JsonNode.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
