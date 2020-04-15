package ru.citeck.ecos.uiserv.service.i18n;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.uiserv.domain.I18nEntity;
import ru.citeck.ecos.uiserv.repository.I18nRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class I18nService {

    private final I18nRepository repo;

    private final Map<String, Map<String, String>> messagesByLocale = new ConcurrentHashMap<>();
    private Map<String, String> defaultMessages = Collections.emptyMap();

    private AtomicBoolean initialized = new AtomicBoolean();
    private String cacheKey;

    public I18nDto upload(I18nDto dto) {

        MandatoryParam.checkString("id", dto.getId());
        MandatoryParam.check("locales", dto.getLocales());
        MandatoryParam.check("messages", dto.getMessages());

        String error = null;

        if (dto.getLocales().size() == 0) {
            error = "Locales is not defined";
        }
        if (StringUtils.isBlank(dto.getId())) {
            error = "ID is not defined";
        }

        if (error != null) {
            throw new IllegalArgumentException(error + ": " + Json.getMapper().toString(dto));
        }

        I18nDto result = toDto(repo.save(toEntity(dto)));
        initialized.set(false);

        return result;
    }

    public String getCacheKey() {
        ensureInitialized();
        return cacheKey;
    }

    public Map<String, String> getMessagesForCurrentLocale() {
        return getMessagesForLocale(null);
    }

    public Map<String, String> getMessagesForLocale(String locale) {

        ensureInitialized();
        if (locale == null) {
            locale = LocaleContextHolder.getLocale().getLanguage().toLowerCase();
        } else {
            if (locale.contains("_")) {
                locale = locale.substring(0, locale.indexOf('_'));
            }
        }
        Map<String, String> result = messagesByLocale.get(locale);
        if (result == null || result.isEmpty()) {
            result = defaultMessages;
        }
        return result;
    }

    public String getMessage(@NonNull String key) {

        ensureInitialized();
        String localeKey = LocaleContextHolder.getLocale().getLanguage();

        String localizedString = null;

        Map<String, String> messages = messagesByLocale.get(localeKey);
        if (messages != null) {
            localizedString = messages.get(key);
        }

        if (MapUtils.isNotEmpty(defaultMessages) && StringUtils.isBlank(localizedString)) {
            localizedString = defaultMessages.get(key);
        }

        return StringUtils.isNotBlank(localizedString) ? localizedString : key;
    }

    private synchronized void ensureInitialized() {
        if (!initialized.get()) {
            registerAll();
            cacheKey = LocalDateTime.now().toString();
            defaultMessages = messagesByLocale.getOrDefault("en", Collections.emptyMap());
            initialized.set(true);
        }
    }

    private synchronized void registerAll() {
        repo.findAll()
            .stream()
            .map(this::toDto)
            .sorted(Comparator.comparing(I18nDto::getOrder))
            .forEach(this::registerMessages);
    }

    private synchronized void registerMessages(I18nDto dto) {

        for (int localeIdx = 0; localeIdx < dto.getLocales().size(); localeIdx++) {

            String locale = dto.getLocales().get(localeIdx).toLowerCase();

            Map<String, String> messages = messagesByLocale.computeIfAbsent(locale, l -> new ConcurrentHashMap<>());

            int finLocIdx = localeIdx;
            dto.getMessages().forEach((key, msgs) -> {
                String message = msgs.size() > finLocIdx ? msgs.get(finLocIdx) : null;
                if (message == null && msgs.size() > 0 && !messages.containsKey(key)) {
                    message = msgs.get(0);
                }
                if (StringUtils.isNotBlank(message)) {
                    messages.put(key, message);
                }
            });
        }
    }

    private I18nEntity toEntity(I18nDto dto) {

        I18nEntity entity = repo.findByTenantAndExtId("", dto.getId()).orElse(null);
        if (entity == null) {
            entity = new I18nEntity();
            entity.setExtId(dto.getId());
        }

        entity.setLocales(Json.getMapper().toString(dto.getLocales()));
        entity.setMessages(Json.getMapper().toString(dto.getMessages()));
        entity.setOrder(dto.getOrder());
        entity.setTenant("");

        return entity;
    }

    private I18nDto toDto(I18nEntity entity) {

        I18nDto dto = new I18nDto();

        dto.setId(entity.getExtId());
        dto.setLocales(Json.getMapper().read(entity.getLocales(), StrList.class));
        dto.setMessages(Json.getMapper().read(entity.getMessages(), StrToStrListMap.class));
        dto.setOrder(entity.getOrder());

        return dto;
    }

    public static class StrList extends ArrayList<String> {
    }

    public static class StrToStrListMap extends HashMap<String, List<String>> {
    }
}
