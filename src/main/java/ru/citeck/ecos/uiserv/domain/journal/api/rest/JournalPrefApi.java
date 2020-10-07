package ru.citeck.ecos.uiserv.domain.journal.api.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.file.repo.FileType;
import ru.citeck.ecos.uiserv.domain.file.service.FileService;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalPrefService;
import ru.citeck.ecos.uiserv.app.web.exception.JournalPrefsNotFoundException;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalSettingsService;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/journalprefs")
@RequiredArgsConstructor
public class JournalPrefApi {

    private final JournalPrefService journalPrefService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    private final JournalSettingsService journalSettingsService;

    /**
     * Now need to use GET method from v2 package.
     */
    @Deprecated
    @GetMapping
    public JsonNode getJournalPrefs(@RequestParam String id,
                                    @ModelAttribute("username") @NonNull String username) {

        JournalSettingsDto settings = journalSettingsService.getById(id);
        if (settings != null) {
            if (!username.equals(settings.getAuthority())) {
                throw new RuntimeException("Access denied");
            }
            return settings.getSettings().getAs(JsonNode.class);
        }
        Optional<JournalPrefService.JournalPreferences> optionalJournalPrefs = journalPrefService.getJournalPrefs(id);
        return optionalJournalPrefs.orElseThrow(() -> new JournalPrefsNotFoundException(id)).getData();
    }

    /**
     * Now need to use PUT method from v2 package.
     */
    @Deprecated
    @PutMapping
    public void putJournalPrefs(@RequestParam String id,
                                @RequestBody byte[] bytes,
                                @ModelAttribute("username") @NonNull String username) {
        validateBody(bytes);

        JournalSettingsDto dto = journalSettingsService.getById(id);
        if (dto == null) {
            fileService.deployFileOverride(FileType.JOURNALPREFS, id, null, bytes);
            return;
        }
        if (!username.equals(dto.getAuthority())) {
            throw new RuntimeException("Access denied");
        }
        dto.setSettings(Json.getMapper().read(bytes, ObjectData.class));

        if (dto.getSettings() != null) {
            dto.setName(dto.getSettings().get("title").asText());
        }
        journalSettingsService.save(dto);
    }

    @DeleteMapping("/id/{journalViewPrefsId}")
    public void deleteJournalPrefs(@PathVariable String journalViewPrefsId,
                                   @ModelAttribute("username") @NonNull String username) {

        JournalSettingsDto settings = journalSettingsService.getById(journalViewPrefsId);
        if (settings != null) {
            if (!username.equals(settings.getAuthority())) {
                throw new RuntimeException("Access denied");
            } else {
                journalSettingsService.delete(journalViewPrefsId);
            }
        } else {
            fileService.deployFileOverride(FileType.JOURNALPREFS, journalViewPrefsId, null, null,
                Collections.emptyMap());
        }
    }

    @PostMapping
    public String postJournalPrefs(@RequestParam String journalId,
                                   @ModelAttribute("username") @NonNull String username,
                                   @RequestBody byte[] bytes) {

        username = validateUsername(username);
        validateBody(bytes);

        JournalSettingsDto journalSettingsDto = new JournalSettingsDto();
        journalSettingsDto.setSettings(Json.getMapper().read(bytes, ObjectData.class));
        journalSettingsDto.setJournalId(journalId);

        if (journalSettingsDto.getSettings() != null) {
            journalSettingsDto.setName(journalSettingsDto.getSettings().get("title").asText());
        }
        journalSettingsDto.setAuthority(username);

        JournalSettingsDto result = journalSettingsService.save(journalSettingsDto);
        return result.getId();
    }

    @GetMapping("/list")
    public List<JournalPrefService.JournalPreferences> getJournalPrefs(@RequestParam String journalId,
                                                                       @ModelAttribute("username") @NonNull
                                                                           String username,
                                                                       @RequestParam(defaultValue = "true")
                                                                           Boolean includeUserLocal) {
        username = validateUsername(username);

        List<JournalSettingsDto> settings = journalSettingsService.getSettings(username, journalId);
        List<JournalPrefService.JournalPreferences> preferences =
            journalPrefService.find(journalId, username, includeUserLocal);

        preferences = preferences == null ? new ArrayList<>() : new ArrayList<>(preferences);
        settings.stream().map(s ->
            new JournalPrefService.JournalPreferences(s.getId(), s.getSettings().getAs(JsonNode.class))
        ).forEach(preferences::add);

        return preferences;
    }

    private String validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.contains("people@")) {
            return username.replaceFirst("people@", "");
        }
        return username;
    }

    private void validateBody(byte[] body) {
        try {
            objectMapper.readValue(body, JsonNode.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
