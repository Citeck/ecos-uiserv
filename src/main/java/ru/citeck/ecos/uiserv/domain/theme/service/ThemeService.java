package ru.citeck.ecos.uiserv.domain.theme.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.io.file.mem.EcosMemDir;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ZipUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.domain.theme.dto.ThemeDto;
import ru.citeck.ecos.uiserv.domain.theme.repo.ThemeEntity;
import ru.citeck.ecos.uiserv.domain.theme.repo.ThemeRepository;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThemeService {

    private final ThemeRepository themeRepo;

    private LoadingCache<StyleKey, byte[]> stylesCache;

    @PostConstruct
    public void init() {
        stylesCache = CacheBuilder.newBuilder()
            .maximumSize(20)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::getStyleImpl));
    }

    public List<ThemeDto> getAll(int max, int skipCount) {

        PageRequest page = PageRequest.of(
            skipCount / max,
            max,
            Sort.by(Sort.Direction.DESC, "id")
        );

        return themeRepo.findAll(page)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public long getCount() {
        return themeRepo.count();
    }

    public List<ThemeDto> getAll(int max, int skipCount, Predicate predicate) {

        PageRequest page = PageRequest.of(
            skipCount / max,
            max,
            Sort.by(Sort.Direction.DESC, "id")
        );

        return themeRepo.findAll(toSpec(predicate), page)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public long getCount(Predicate predicate) {
        return themeRepo.count(toSpec(predicate));
    }

    public ThemeDto deploy(ThemeDto themeDto) {
        ThemeEntity entity = toEntity(themeDto);
        if (entity != null) {
            try {
                return toDto(themeRepo.save(entity));
            } finally {
                stylesCache.invalidateAll();
            }
        }
        return null;
    }

    public void delete(String id) {
        if (!"ecos".equals(id)) {
            themeRepo.findFirstByExtId(id).ifPresent(themeRepo::delete);
            stylesCache.invalidateAll();
        }
    }

    @NotNull
    public byte[] getStyle(String themeId, String name) {
        return stylesCache.getUnchecked(new StyleKey(themeId, name));
    }

    @NotNull
    private byte[] getStyleImpl(StyleKey key) {

        if (StringUtils.isBlank(key.getName()) || StringUtils.isBlank(key.getThemeId())) {
            return new byte[0];
        }
        ThemeDto dto = toDto(themeRepo.findFirstByExtId(key.getThemeId()).orElse(null));
        if (dto == null) {
            return new byte[0];
        }
        String name = key.getName();
        if (name.endsWith(".css")) {
            name = name.replaceAll("\\.css$", "");
        }
        byte[] data = dto.getStyles().get(name);
        return data != null ? data : new byte[0];
    }

    public String getCacheKey() {
        return themeRepo.getLastModifiedTime()
            .map(Instant::toEpochMilli)
            .orElse(0L) + "-" + themeRepo.count();
    }

    @Nullable
    public ThemeDto getTheme(String id) {
        return toDto(themeRepo.findFirstByExtId(id).orElse(null));
    }

    public ThemeDto getWithStyles(String id) {
        return toDto(themeRepo.findFirstByExtId(id).orElse(null));
    }

    private ThemeDto toDto(ThemeEntity entity) {

        if (entity == null) {
            return null;
        }

        ThemeDto dto = new ThemeDto();

        dto.setId(entity.getExtId());
        dto.setImages(Json.getMapper().readMap(entity.getImages(), String.class, RecordRef.class));
        dto.setName(Json.getMapper().read(entity.getName(), MLText.class));

        Map<String, byte[]> styles = new HashMap<>();
        dto.setStyles(styles);

        byte[] stylesData = entity.getStyles();
        if (stylesData == null) {
            return dto;
        }
        EcosMemDir stylesDir = ZipUtils.extractZip(stylesData);
        stylesDir.getChildren().forEach(child -> {
            String name = child.getName().replaceAll("\\.css$", "");
            styles.put(name, child.readAsBytes());
        });

        return dto;
    }

    private ThemeEntity toEntity(ThemeDto dto) {

        if (dto == null) {
            return null;
        }

        ThemeEntity entity;
        String extId = dto.getId();
        if (StringUtils.isNotBlank(extId)) {
            entity = themeRepo.findFirstByExtId(extId).orElseGet(() -> {
                ThemeEntity newEntity = new ThemeEntity();
                newEntity.setExtId(extId);
                newEntity.setTenant("");
                return newEntity;
            });
        } else {
            entity = new ThemeEntity();
            entity.setExtId(UUID.randomUUID().toString());
            entity.setTenant("");
        }

        entity.setImages(Json.getMapper().toString(dto.getImages()));
        entity.setName(Json.getMapper().toString(dto.getName()));

        Map<String, byte[]> styles = dto.getStyles();
        if (styles == null) {
            entity.setStyles(null);
        } else {
            EcosMemDir stylesDir = new EcosMemDir();
            styles.forEach((name, data) -> stylesDir.createFile(name + ".css", data));
            entity.setStyles(ZipUtils.writeZipAsBytes(stylesDir));
        }

        return entity;
    }

    private Specification<ThemeEntity> toSpec(Predicate predicate) {

        PredicateDto predicateDto = PredicateUtils.convertToDto(predicate, PredicateDto.class);
        Specification<ThemeEntity> spec = null;

        if (org.apache.commons.lang3.StringUtils.isNotBlank(predicateDto.moduleId)) {
            spec = (root, query, builder) ->
                builder.like(builder.lower(root.get("extId")), "%" + predicateDto.moduleId.toLowerCase() + "%");
        }

        return spec;
    }

    @Data
    public static class PredicateDto {
        private String moduleId;
    }

    @Data
    @AllArgsConstructor
    private static final class StyleKey {
        private String themeId;
        private String name;
    }
}
