package ru.citeck.ecos.uiserv.web.rest.v2;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.service.journal.JournalPrefService;
import ru.citeck.ecos.uiserv.web.rest.errors.JournalPrefsNotFoundException;

import java.util.Optional;

@RestController
@RequestMapping("/api/journalprefs")
@Slf4j
@RequiredArgsConstructor
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
