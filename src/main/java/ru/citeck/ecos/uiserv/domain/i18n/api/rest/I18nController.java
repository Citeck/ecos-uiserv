package ru.citeck.ecos.uiserv.domain.i18n.api.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.i18n.service.I18nService;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class I18nController {

    private final I18nService i18nService;

    @GetMapping(path = "/locale", produces = MediaType.APPLICATION_JSON_VALUE)
    public HttpEntity<byte[]> getMessagesByLocale(@RequestParam(required = false, name = "id") String id) {

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(4, TimeUnit.HOURS)
            .mustRevalidate()
            .cachePublic()
        );

        return new HttpEntity<>(Json.getMapper().toBytes(i18nService.getMessagesForLocale(id)), headers);
    }

    @GetMapping(value = "/cache-key", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getCacheKey() {
        return i18nService.getCacheKey();
    }
}
