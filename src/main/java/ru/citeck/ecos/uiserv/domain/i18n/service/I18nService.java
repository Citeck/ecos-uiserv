package ru.citeck.ecos.uiserv.domain.i18n.service;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.domain.i18n.repo.I18nEntity;
import ru.citeck.ecos.uiserv.domain.i18n.dto.I18nDto;
import ru.citeck.ecos.uiserv.domain.i18n.repo.I18nRepository;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class I18nService {

    private final I18nRepository repo;

    private final Map<String, Map<String, String>> messagesByLocale = new ConcurrentHashMap<>();
    private Map<String, String> defaultMessages = Collections.emptyMap();

    private final AtomicBoolean initialized = new AtomicBoolean();
    private String cacheKey;

    @Nullable
    public I18nDto getById(String id) {
        return toDto(repo.findByTenantAndExtId("", id).orElse(null));
    }

    public List<I18nDto> getAll(int max, int skipCount) {

        PageRequest page = PageRequest.of(
            skipCount / max,
            max,
            Sort.by(Sort.Direction.DESC, "id")
        );

        return repo.findAll(page)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public long getCount() {
        return repo.count();
    }

    public List<I18nDto> getAll(int max, int skipCount, Predicate predicate) {

        PageRequest page = PageRequest.of(
            skipCount / max,
            max,
            Sort.by(Sort.Direction.DESC, "id")
        );

        return repo.findAll(toSpec(predicate), page)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public long getCount(Predicate predicate) {
        return repo.count(toSpec(predicate));
    }

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

    public void delete(String id) {
        repo.findByTenantAndExtId("", id).ifPresent(entity -> {
            repo.delete(entity);
            initialized.set(false);
        });
    }

    private synchronized void ensureInitialized() {
        if (!initialized.get()) {
            messagesByLocale.clear();
            defaultMessages = Collections.emptyMap();
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

    private I18nDto toDto(@Nullable I18nEntity entity) {

        if (entity == null) {
            return null;
        }

        I18nDto dto = new I18nDto();

        dto.setId(entity.getExtId());
        dto.setLocales(Json.getMapper().read(entity.getLocales(), StrList.class));
        dto.setMessages(Json.getMapper().read(entity.getMessages(), StrToStrListMap.class));
        dto.setOrder(entity.getOrder());

        return dto;
    }

    private Specification<I18nEntity> toSpec(Predicate predicate) {

        PredicateDto predicateDto = PredicateUtils.convertToDto(predicate, PredicateDto.class);
        Specification<I18nEntity> spec = null;

        if (StringUtils.isNotBlank(predicateDto.moduleId)) {
            spec = (root, query, builder) ->
                builder.like(builder.lower(root.get("extId")), "%" + predicateDto.moduleId.toLowerCase() + "%");
        }

        return spec;
    }

    public static class StrList extends ArrayList<String> {
    }

    public static class StrToStrListMap extends HashMap<String, List<String>> {
    }

    @Data
    public static class PredicateDto {
        private String moduleId;
    }
}
