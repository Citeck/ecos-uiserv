package ru.citeck.ecos.uiserv.domain.journal.api.rest;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.uiserv.app.web.exception.JournalPrefsNotFoundException;
import ru.citeck.ecos.uiserv.domain.file.repo.FileType;
import ru.citeck.ecos.uiserv.domain.file.service.FileService;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalPrefService;

import java.util.Optional;

/**
 * @deprecated use ru.citeck.ecos.uiserv.domain.journal.api.records.JournalSettingsRecordsDao
 */
@RestController
@RequestMapping("/api/journalprefs")
@Slf4j
@RequiredArgsConstructor
@Deprecated
public class JournalPreferencesApi {

    private final JournalPrefService journalPrefService;
    private final FileService fileService;

    @GetMapping("/{id}")
    public JsonNode getJournalPrefs(@PathVariable("id") String id) {
        Optional<JournalPrefService.JournalPreferences> optionalJournalPrefs = journalPrefService.getJournalPrefs(id);
        return optionalJournalPrefs.orElseThrow(() -> new JournalPrefsNotFoundException(id)).getData();
    }

    @PutMapping("/{id}")
    public void putJournalPrefs(@PathVariable("id") String id,
                                @RequestBody byte[] bytes) {
        fileService.deployFileOverride(FileType.JOURNALPREFS, id, null, bytes);
    }
}
