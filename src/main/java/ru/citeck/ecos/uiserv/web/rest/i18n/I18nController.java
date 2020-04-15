package ru.citeck.ecos.uiserv.web.rest.i18n;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.service.i18n.I18nService;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class I18nController {

    private final I18nService i18nService;

    @GetMapping(path = "/locale", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public HttpEntity<byte[]> getMessagesByLocale(@RequestParam(required = false) String id) {

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(100, TimeUnit.MINUTES).mustRevalidate());

        return new HttpEntity<>(Json.getMapper().toBytes(i18nService.getMessagesForLocale(id)), headers);
    }

    @GetMapping(value = "/cache-key", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getCacheKey() {
        return i18nService.getCacheKey();
    }
}
