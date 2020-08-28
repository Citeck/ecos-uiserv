package ru.citeck.ecos.uiserv.domain.theme.api.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/theme")
public class ThemeController {

    private final ThemeService themeService;

    @GetMapping(path = "/{themeId}/style/{styleId}", produces = "text/css")
    public HttpEntity<byte[]> getMessagesByLocale(@PathVariable("themeId") String themeId,
                                                  @PathVariable("styleId") String styleId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(4, TimeUnit.HOURS)
            .mustRevalidate()
            .cachePublic()
        );
        return new HttpEntity<>(themeService.getStyle(themeId, styleId), headers);
    }
}
