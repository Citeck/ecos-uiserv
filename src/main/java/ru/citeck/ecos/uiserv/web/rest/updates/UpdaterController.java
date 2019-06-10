package ru.citeck.ecos.uiserv.web.rest.updates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileBundle;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.service.translation.TranslationService;
import ru.citeck.ecos.uiserv.web.rest.JournalPrefApi;

import java.io.IOException;

@RestController
@RequestMapping("/api/updates")
@Transactional
@RequiredArgsConstructor
public class UpdaterController {
    private final FileService fileService;

    private final TranslationService i18n;

    private final JournalPrefApi journalPrefApi;

    private final ObjectMapper objectMapper;

    private static String MIME_TYPE_LOWERCASE = "application/zip";

    @PostMapping
    public String deploy(ModuleToDeploy update) {
        if (!update.mimeType.toLowerCase().equals(MIME_TYPE_LOWERCASE))
            throw new IllegalArgumentException("Expected " + MIME_TYPE_LOWERCASE + " 'data' entry");
        final FileType fileType = FileType.valueOf(update.type);

        if (fileType != FileType.JOURNALPREFS) {
            final FileBundle bundle = fileService.readBundleFromZip(update.data, fileType);
            final String fileId = fileService.getFileMetadataExtractor(fileType)
                .getFileId(bundle.bytes);
            final File deployed = fileService.deployStandardFile(
                fileType, fileId, update.mimeType, bundle.bytes, update.version);
            // Should be always present, unless we are deploying "removal" of the menu;
            // in that special case we should not load translations.
            bundle.translations.forEach((tag, dictionary) -> i18n.saveTranslations(
                deployed.getFileVersion().getTranslated().getId(), tag, dictionary));
            return fileId;
        } else {
            //special processing for prefs because we not just deploy a file but
            //also store additional metadata (like, what journal these prefs are bound to)
            try {
                final JournalPrefsUpdate journalPrefsUpdate =
                    objectMapper.readValue(update.data, JournalPrefsUpdate.class);
                journalPrefApi.deployJournalPrefs(journalPrefsUpdate.getId(),
                    journalPrefsUpdate.getJournalId(), journalPrefsUpdate.getData());
                return journalPrefsUpdate.getId();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Getter
    @Setter
    public static class JournalPrefsUpdate {
        private String id;
        private String journalId;
        private JsonNode data;
    }
}
