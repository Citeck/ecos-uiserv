package ru.citeck.ecos.uiserv.domain.i18n.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.app.common.perms.UiServSystemArtifactPerms;
import ru.citeck.ecos.uiserv.domain.i18n.api.records.I18nRecords;
import ru.citeck.ecos.uiserv.domain.i18n.repo.I18nEntity;
import ru.citeck.ecos.uiserv.domain.i18n.dto.I18nDto;
import ru.citeck.ecos.uiserv.domain.i18n.repo.I18nRepository;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class I18nService implements MessageResolver {

    private final I18nRepository repo;
    private final UiServSystemArtifactPerms perms;

    private final Map<String, Map<String, String>> messagesByLocale = new ConcurrentHashMap<>();
    private Map<String, String> defaultMessages = Collections.emptyMap();

    private final JpaSearchConverterFactory jpaSearchConverterFactory;
    private JpaSearchConverter<I18nEntity> searchConv;

    private final AtomicBoolean initialized = new AtomicBoolean();
    private String cacheKey;

    private final List<BiConsumer<I18nDto, I18nDto>> listeners = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        searchConv = jpaSearchConverterFactory.createConverter(I18nEntity.class).build();
    }

    @Nullable
    public I18nDto getById(String id) {
        return toDto(repo.findByTenantAndExtId("", id).orElse(null));
    }

    public long getLastModifiedTimeMs() {
        return repo.getLastModifiedTime()
            .map(Instant::toEpochMilli)
            .orElse(0L);
    }

    public List<I18nDto> getAll(int max, int skipCount) {

        if (max == 0) {
            return Collections.emptyList();
        }

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

    public List<I18nDto> getAll(Predicate predicate, int max, int skip, List<SortBy> sort) {
        return searchConv.findAll(repo, predicate, max, skip, sort)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public long getCount(Predicate predicate) {
        return searchConv.getCount(repo, predicate);
    }

    public I18nDto upload(I18nDto dto) {
        perms.checkWrite(EntityRef.create(AppName.UISERV, I18nRecords.ID, dto.getId()));

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

        I18nDto before = repo.findByTenantAndExtId("", dto.getId())
            .map(this::toDto)
            .orElse(null);

        I18nDto result = toDto(repo.save(toEntity(dto)));
        initialized.set(false);

        for (BiConsumer<I18nDto, I18nDto> listener : listeners) {
            listener.accept(before, result);
        }

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
        perms.checkWrite(EntityRef.create(AppName.UISERV, I18nRecords.ID, id));

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
            cacheKey = getLastModifiedTimeMs() + "-" + getCount();
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
                if (message == null && !msgs.isEmpty() && !messages.containsKey(key)) {
                    message = msgs.getFirst();
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

    public void addListener(BiConsumer<I18nDto, I18nDto> listener) {
        listeners.add(listener);
    }

    public static class StrList extends ArrayList<String> {
    }

    public static class StrToStrListMap extends HashMap<String, List<String>> {
    }
}
