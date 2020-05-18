package ru.citeck.ecos.uiserv.web.rest.v1;

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
import ru.citeck.ecos.uiserv.service.journal.JournalPrefService;
import ru.citeck.ecos.uiserv.web.rest.v1.dto.ModuleToDeploy;

import java.io.IOException;

@RestController
@RequestMapping("/api/updates")
@Transactional
@RequiredArgsConstructor
public class UpdaterApi {

    private final FileService fileService;

    private final JournalPrefService journalPrefsService;

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
            return fileId;
        } else {
            //special processing for prefs because we not just deploy a file but
            //also store additional metadata (like, what journal these prefs are bound to)
            try {
                final JournalPrefsUpdate journalPrefsUpdate =
                    objectMapper.readValue(update.data, JournalPrefsUpdate.class);
                journalPrefsService.deployJournalPrefs(journalPrefsUpdate.getId(),
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
