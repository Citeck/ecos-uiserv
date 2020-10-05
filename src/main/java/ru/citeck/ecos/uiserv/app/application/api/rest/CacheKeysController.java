package ru.citeck.ecos.uiserv.app.application.api.rest;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ui-cache")
@Transactional
@RequiredArgsConstructor
public class CacheKeysController {

    private final RecordsService recordsService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public HttpEntity<byte[]> getCache(@RequestParam(required = false) String types) {

        if (types == null) {
            types = "";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache());

        Map<String, String> cacheAtts = new HashMap<>();
        Arrays.stream(types.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .forEach(a -> cacheAtts.put(a, "attributes." + a + "-cache-key"));

        if (cacheAtts.isEmpty()) {
            return new HttpEntity<>("{}".getBytes(StandardCharsets.UTF_8), headers);
        }

        RecordMeta attributes = recordsService.getAttributes(RecordRef.valueOf("meta@"), cacheAtts);
        return new HttpEntity<>(Json.getMapper().toBytes(attributes.getAttributes()), headers);
    }
}
