package ru.citeck.ecos.uiserv.domain.journal.api.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.app.common.service.AuthoritiesSupport;
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils;
import ru.citeck.ecos.uiserv.app.web.exception.JournalPrefsNotFoundException;
import ru.citeck.ecos.uiserv.domain.file.repo.FileType;
import ru.citeck.ecos.uiserv.domain.file.service.FileService;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalPrefService;
import ru.citeck.ecos.uiserv.domain.journal.service.settings.JournalSettingsService;

import java.io.IOException;
import java.util.*;

/**
 * @deprecated use ru.citeck.ecos.uiserv.domain.journal.api.records.JournalSettingsRecordsDao
 */
@RestController
@RequestMapping("/api/journalprefs")
@RequiredArgsConstructor
@Deprecated
public class JournalPrefApi {

    private final JournalPrefService journalPrefService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final AuthoritiesSupport authoritiesSupport;

    private final JournalSettingsService journalSettingsService;

    /**
     * Now need to use GET method from v2 package.
     */
    @Deprecated
    @GetMapping
    public JsonNode getJournalPrefs(@RequestParam String id) {

        String username = SecurityUtils.getCurrentUserLoginFromRequestContext();
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
                                @RequestBody byte[] bytes) {
        validateBody(bytes);

        JournalSettingsDto dto = journalSettingsService.getById(id);
        if (dto == null) {
            fileService.deployFileOverride(FileType.JOURNALPREFS, id, null, bytes);
            return;
        }

        String username = SecurityUtils.getCurrentUserLoginFromRequestContext();

        if (!Objects.equals(username, dto.getAuthority())) {
            throw new RuntimeException("Access denied");
        }

        JournalSettingsDto.Builder builder = dto.copy();
        ObjectData settings = Json.getMapper().read(bytes, ObjectData.class);
        builder.withSettings(settings);

        if (settings != null) {
            String title = settings.get("title").asText();
            builder.withName(Json.getMapper().read(title, MLText.class));
        }
        journalSettingsService.save(builder.build());
    }

    @DeleteMapping("/id/{journalViewPrefsId}")
    public void deleteJournalPrefs(@PathVariable String journalViewPrefsId) {

        String username = SecurityUtils.getCurrentUserLoginFromRequestContext();

        JournalSettingsDto settings = journalSettingsService.getById(journalViewPrefsId);
        if (settings != null) {
            if (!Objects.equals(username, settings.getAuthority())) {
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
                                   @RequestBody byte[] bytes) {

        String username = SecurityUtils.getCurrentUserLoginFromRequestContext();

        username = validateUsername(username);
        validateBody(bytes);

        ObjectData settings = Json.getMapper().read(bytes, ObjectData.class);
        JournalSettingsDto.Builder builder = JournalSettingsDto.create()
                .withSettings(settings)
                .withJournalId(journalId)
                .withAuthority(username);
        if (settings != null) {
            String title = settings.get("title").asText();
            builder.withName(Json.getMapper().read(title, MLText.class));
        }

        JournalSettingsDto result = journalSettingsService.save(builder.build());
        return result.getId();
    }

    @GetMapping("/list")
    public List<JournalPrefService.JournalPreferences> getJournalPrefs(@RequestParam String journalId,
                                                                       @RequestParam(defaultValue = "true")
                                                                           Boolean includeUserLocal) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

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
