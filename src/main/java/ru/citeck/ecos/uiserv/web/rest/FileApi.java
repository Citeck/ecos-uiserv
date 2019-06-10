package ru.citeck.ecos.uiserv.web.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.citeck.ecos.uiserv.config.ApplicationProperties;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.service.file.FileBundle;
import ru.citeck.ecos.uiserv.service.translation.TranslationService;
import ru.citeck.ecos.uiserv.web.rest.menu.resolvers.MenuItemsResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/store/{fileType}")
@Transactional
public class FileApi {
    @Autowired
    private List<MenuItemsResolver> resolvers;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private TranslationService i18n;

    @Autowired
    private FileService fileService;

    @GetMapping
    public HttpEntity<byte[]> get(@PathVariable FileType fileType, @RequestParam String fileId) {
        //todo optionally extract as zip bundle, body plus translations
        return fileService.loadFile(fileType, fileId).map(f -> ResponseEntity.ok(f.getFileVersion().getBytes()))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/{fileId}/deployForm")
    public void deploy(@PathVariable FileType fileType, @PathVariable String fileId,
                       @RequestParam long productVersion,
                       @RequestParam(required = false) MultipartFile zip,
                       @RequestParam(required = false) MultipartFile file) throws IOException {
        if ((zip == null) != (file == null))
            throw new IllegalArgumentException("Upload requires either single file as 'file' parameter or zipped bundle as 'zip'");
        if (zip != null) {
            final FileBundle bundle = fileService.readBundleFromZip(zip.getBytes(), fileType);
            deploy(fileType, fileId, productVersion,
                null /*don't specify content type*/, bundle.bytes, bundle.translations);
            return;
        }
        if (file != null) {
            deploy(fileType, fileId, productVersion, file.getContentType(), file.getBytes(), Collections.emptyMap());
            return;
        }
    }


    @PostMapping("/{fileId}/deploy")
    public void deploy(@PathVariable FileType fileType, @PathVariable String fileId,
                       @RequestParam long productVersion,
                       @RequestParam(defaultValue = "false") boolean bundle,
                       @RequestHeader("content-type") String contentType,
                       @RequestBody byte[] bytes) throws IOException {
        if (bundle) {
            final FileBundle b = fileService.readBundleFromZip(bytes, fileType);
            deploy(fileType, fileId, productVersion,
                null /*don't specify content type*/, b.bytes, b.translations);
            return;
        } else {
            deploy(fileType, fileId, productVersion, contentType, bytes, Collections.emptyMap());
            return;
        }
    }

    private void deploy(@PathVariable FileType fileType, String fileId,
                        @RequestParam long productVersion, String contentType,
                        byte[] bytes, Map<String, byte[]> translations) throws IOException {
        //final String fileId = fileService.getFileMetadataExtractor(fileType).getFileId(bytes);
        final File deployed = fileService.deployStandardFile(fileType,
            fileId, contentType, bytes, productVersion);

        //   final Optional<MenuService.MenuView> menuView = menuService.deployStandardMenu(
        //     menuBundle.xml.getId(), menuBundle.xml, productVersion);

        // Should be always present, unless we are deploying "removal" of the menu;
        // in that special case we should not load translations.
        translations.forEach((tag, trBytes) ->
            i18n.saveTranslations(deployed.getFileVersion().getTranslated().getId(), tag, trBytes));
    }

    @PostMapping("/{fileId}/revert")
    public void revert(@PathVariable FileType fileType, @PathVariable String fileId) {
        fileService.revertFileOverrides(fileType, fileId);
    }

    //Basically same as /override, but the latter gets file-id from zip and hence cannot work
    //without zip, therefore cannot accept bytes=null
    @PostMapping("/{fileId}/overrideWithEmpty")
    public void overrideWithEmpty(@PathVariable FileType fileType, @PathVariable String fileId) {
        fileService.deployFileOverride(fileType, fileId, null, null, null);
    }

    @PostMapping("/{fileId}/overrideForm")
    public void override(@PathVariable FileType fileType, @PathVariable String fileId,
                         @RequestParam(required = false) MultipartFile zip,
                         @RequestParam(required = false) MultipartFile file) throws IOException {
        if ((zip == null) == (file == null))
            throw new IllegalArgumentException("Upload requires either single file as 'file' parameter or zipped bundle as 'zip'");

        if (zip != null) {
            final FileBundle bundle = fileService.readBundleFromZip(zip.getBytes(), fileType);
            override(fileType, fileId, null /*don't specify content type*/, bundle.bytes, bundle.translations);
            return;
        }
        if (file != null) {
            override(fileType, fileId, file.getContentType(), file.getBytes(), Collections.emptyMap());
            return;
        }
    }

    @PostMapping("/{fileId}/override")
    public void override(@PathVariable FileType fileType, @PathVariable String fileId,
                         @RequestParam(defaultValue = "false") boolean bundle,
                         @RequestHeader("content-type") String contentType,
                         @RequestBody byte[] bytes) throws IOException {
        if (bundle) {
            final FileBundle b = fileService.readBundleFromZip(bytes, fileType);
            override(fileType, fileId, null /*don't specify content type*/, b.bytes, b.translations);
            return;
        } else {
            override(fileType, fileId, contentType, bytes, Collections.emptyMap());
            return;
        }
    }

    private void override(@PathVariable FileType fileType, String fileId, String contentType, byte[] bytes, Map<String, byte[]> translations) {
        //final String fileId = fileService.getFileMetadataExtractor(fileType).getFileId(bytes);

        final File deployed = fileService.deployFileOverride(fileType, fileId, contentType, bytes, null);

        // Should be always present, unless we are deploying "removal" of the menu;
        // in that special case we should not load translations.
        translations.forEach((tag, trBytes) ->
            i18n.saveTranslations(deployed.getFileVersion().getTranslated().getId(), tag, trBytes));
    }

}
