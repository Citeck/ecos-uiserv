package ru.citeck.ecos.uiserv.domain.theme.api.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.commons.io.file.std.EcosStdFile;
import ru.citeck.ecos.uiserv.domain.theme.dto.ResourceData;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/theme")
public class ThemeController {

    private final ThemeService themeService;

    private byte[] notFoundImageData;
    private MediaType notFoundImageType;

    @PostConstruct
    public void init() throws FileNotFoundException {

        File notFoundFile = ResourceUtils.getFile("classpath:image/not-found.png");

        EcosStdFile ecosFile = new EcosStdFile(notFoundFile);
        notFoundImageData = ecosFile.readAsBytes();
        notFoundImageType = guessMediaType(notFoundFile.getName());
    }

    @GetMapping(path = "/current", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getCurrentTheme() {
        return themeService.getActiveTheme();
    }

    @GetMapping(path = "/{themeId}/style/{styleId}", produces = "text/css;charset=UTF-8")
    public HttpEntity<byte[]> getThemeStyle(@PathVariable("themeId") String themeId,
                                            @PathVariable("styleId") String styleId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(4, TimeUnit.HOURS)
            .mustRevalidate()
            .cachePublic()
        );

        ResourceData data = themeService.getStyle(themeId, styleId);
        byte[] bytes = data.getData();
        if (bytes == null || bytes.length == 0) {
            bytes = ".undefined-style {}".getBytes(StandardCharsets.UTF_8);
        }
        return new HttpEntity<>(bytes, headers);
    }

    @GetMapping(path = "/{themeId}/font/{*fontId}")
    public HttpEntity<byte[]> getThemeFont(@PathVariable("themeId") String themeId,
                                           @PathVariable("fontId") String fontId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(4, TimeUnit.HOURS)
            .mustRevalidate()
            .cachePublic()
        );

        ResourceData data = themeService.getFont(themeId, fontId);
        byte[] bytes = data.getData();
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        headers.setContentType(getFontMediaType(fontId));
        headers.setContentDisposition(ContentDisposition.empty());
        return new HttpEntity<>(bytes, headers);
    }

    @GetMapping(path = "/{themeId}/image/{imageId}")
    public HttpEntity<byte[]> getThemeImage(@PathVariable("themeId") String themeId,
                                            @PathVariable("imageId") String imageId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(4, TimeUnit.HOURS)
            .mustRevalidate()
            .cachePublic()
        );

        ResourceData data = themeService.getImage(themeId, imageId);
        byte[] bytes = data.getData();
        if (bytes == null || bytes.length == 0) {
            return getNotFoundImageEntity(headers);
        }

        MediaType mediaType = guessMediaType(data.getFileName());
        headers.setContentType(mediaType);

        return new HttpEntity<>(bytes, headers);
    }

    private MediaType guessMediaType(String fileName) {
        if (fileName.endsWith("jpg") || fileName.endsWith("jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (fileName.endsWith("png")) {
            return MediaType.IMAGE_PNG;
        } else if (fileName.endsWith("ico")) {
            return MediaType.valueOf("image/x-icon");
        } else if (fileName.endsWith("svg")) {
            return MediaType.valueOf("image/svg+xml");
        } else {
            throw new IllegalStateException("Unknown image type: " + fileName);
        }
    }

    private MediaType getFontMediaType(String fileName) {
        //see ThemeService.RES_EXTENSIONS
        String fontType = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (fontType) {
            case "woff2" -> MediaType.parseMediaType("font/woff2");
            case "woff" -> MediaType.parseMediaType("font/woff");
            case "ttf" -> MediaType.parseMediaType("font/ttf");
            case "otf" -> MediaType.parseMediaType("font/otf");
            case "eot" -> MediaType.parseMediaType("application/vnd.ms-fontobject");
            default -> throw new IllegalStateException("Unknown font type: " + fontType);
        };
    }

    public HttpEntity<byte[]> getNotFoundImageEntity(HttpHeaders headers) {
        headers.setContentType(notFoundImageType);
        return new HttpEntity<>(notFoundImageData, headers);
    }
}
