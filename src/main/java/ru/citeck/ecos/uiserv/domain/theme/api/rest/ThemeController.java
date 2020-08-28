package ru.citeck.ecos.uiserv.domain.theme.api.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.commons.io.file.std.EcosStdFile;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.icon.api.records.IconRecords;
import ru.citeck.ecos.uiserv.domain.icon.dto.IconDto;
import ru.citeck.ecos.uiserv.domain.icon.service.IconService;
import ru.citeck.ecos.uiserv.domain.theme.dto.ThemeDto;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/theme")
public class ThemeController {

    private final ThemeService themeService;
    private final IconService iconService;

    private byte[] notFoundImageData;
    private MediaType notFoundImageType;

    @PostConstruct
    public void init() throws FileNotFoundException {

        File notFoundFile = ResourceUtils.getFile("classpath:image/not-found.png");

        EcosStdFile ecosFile = new EcosStdFile(notFoundFile);
        notFoundImageData = ecosFile.readAsBytes();
        notFoundImageType = guessMediaType(notFoundFile.getName());
    }

    @GetMapping(path = "/{themeId}/style/{styleId}", produces = "text/css")
    public HttpEntity<byte[]> getThemeStyle(@PathVariable("themeId") String themeId,
                                            @PathVariable("styleId") String styleId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(4, TimeUnit.HOURS)
            .mustRevalidate()
            .cachePublic()
        );
        return new HttpEntity<>(themeService.getStyle(themeId, styleId), headers);
    }

    @GetMapping(path = "/{themeId}/image/{imageId}")
    public HttpEntity<byte[]> getThemeImage(@PathVariable("themeId") String themeId,
                                            @PathVariable("imageId") String imageId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(4, TimeUnit.HOURS)
            .mustRevalidate()
            .cachePublic()
        );

        ThemeDto theme = themeService.getTheme(themeId);

        if (theme == null || theme.getImages() == null)  {
            return getNotFoundImageEntity(headers);
        }

        RecordRef imageRef = theme.getImages().get(imageId);
        if (RecordRef.isEmpty(imageRef)) {
            return getNotFoundImageEntity(headers);
        }

        if (imageRef.getSourceId().equals(IconRecords.ID)) {
            IconDto iconDto = iconService.findById(imageRef.getId()).orElse(null);
            if (iconDto == null) {
                return getNotFoundImageEntity(headers);
            }
            byte[] bytes = iconDto.getByteData();
            MediaType mediaType = iconDto.getMimetype() != null ?
                MediaType.valueOf(iconDto.getMimetype().toString()) : guessMediaType(iconDto.getId());
            headers.setContentType(mediaType);
            return new HttpEntity<>(bytes, headers);
        }

        log.error("Unsupported image ref: " + imageRef);
        return getNotFoundImageEntity(headers);
    }

    private MediaType guessMediaType(String fileName) {
        if (fileName.endsWith("jpg") || fileName.endsWith("jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (fileName.endsWith("png")) {
            return MediaType.IMAGE_PNG;
        } else {
            throw new IllegalStateException("Unknown image type: " + fileName);
        }
    }

    private HttpEntity<byte[]> getNotFoundImageEntity(HttpHeaders headers) {
        headers.setContentType(notFoundImageType);
        return new HttpEntity<>(notFoundImageData, headers);
    }
}
