package ru.citeck.ecos.uiserv.domain.theme.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.io.file.mem.EcosMemDir;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ZipUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.domain.icon.api.records.IconRecords;
import ru.citeck.ecos.uiserv.domain.icon.dto.IconDto;
import ru.citeck.ecos.uiserv.domain.icon.service.IconService;
import ru.citeck.ecos.uiserv.domain.theme.dto.ResourceData;
import ru.citeck.ecos.uiserv.domain.theme.dto.ThemeDto;
import ru.citeck.ecos.uiserv.domain.theme.repo.ThemeEntity;
import ru.citeck.ecos.uiserv.domain.theme.repo.ThemeRepository;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThemeService {

    public static final String DEFAULT_THEME_ID = "citeck-theme-default";
    public static final String NULL_THEME_ID = "null";

    public static final String CURRENT_THEME_CONFIG_KEY = "active-theme";
    public static final String RES_TYPE_IMAGE = "image";
    public static final String ACTIVE_THEME_ID = "active";
    public static final String RES_TYPE_STYLE = "style";
    public static final String ICON_REF_PREFIX = "uiserv/" + IconRecords.ID + "@";

    // ThemeController.groovy should has the same constants
    public static final List<String> RES_EXTENSIONS = Arrays.asList("png", "jpeg", "jpg", "ico", "svg", "css");

    private static final ResourceData EMPTY_RESOURCE = new ResourceData(null, null);

    private final ThemeRepository themeRepo;
    private final RecordsService recordsService;
    private final IconService iconService;

    private final JpaSearchConverterFactory jpaSearchConverterFactory;
    private JpaSearchConverter<ThemeEntity> searchConv;

    private LoadingCache<ResourceKey, ResourceData> resourcesCache;
    private LoadingCache<String, String> activeThemeCache;
    private String prevActiveTheme = null;

    @PostConstruct
    public void init() {

        resourcesCache = CacheBuilder.newBuilder()
            .maximumSize(20)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::getInhResourceImpl));
        activeThemeCache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::getActiveThemeImpl));

        searchConv = jpaSearchConverterFactory.createConverter(ThemeEntity.class).build();
    }

    public String getActiveTheme() {
        String res = activeThemeCache.getUnchecked(ACTIVE_THEME_ID);
        if (!Objects.equals(prevActiveTheme, res)) {
            resourcesCache.invalidateAll();
            prevActiveTheme = res;
        }
        return res;
    }

    private String getActiveThemeImpl() {

        String theme = recordsService.getAtt(
            EntityRef.create("cfg", CURRENT_THEME_CONFIG_KEY), "value?str"
        ).asText();

        var fixedTheme = new StringBuilder(theme.length());
        for (int i = 0; i < theme.length(); i++) {
            var ch = theme.charAt(i);
            if (!Character.isWhitespace(ch)) {
                fixedTheme.append(ch);
            }
        }
        theme = fixedTheme.toString();

        if (StringUtils.isBlank(theme)) {
            theme = DEFAULT_THEME_ID;
        }

        return theme;
    }

    public List<ThemeDto> getAll(int max, int skipCount) {

        if (max == 0) {
            return Collections.emptyList();
        }

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

    public List<ThemeDto> getAll(Predicate predicate, int max, int skip, List<SortBy> sort) {
        return searchConv.findAll(themeRepo, predicate, max, skip, sort)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public long getCount(Predicate predicate) {
        return searchConv.getCount(themeRepo, predicate);
    }

    public ThemeDto deploy(ThemeDto themeDto) {
        ThemeEntity entity = toEntity(themeDto);
        if (entity != null) {
            try {
                return toDto(themeRepo.save(entity));
            } finally {
                resourcesCache.invalidateAll();
            }
        }
        return null;
    }

    public void delete(String id) {
        if (!DEFAULT_THEME_ID.equals(id)) {
            themeRepo.findFirstByExtId(id).ifPresent(themeRepo::delete);
            resourcesCache.invalidateAll();
        }
    }

    @NotNull
    public ResourceData getStyle(String themeId, String name) {
        if (StringUtils.isBlank(name)) {
            return EMPTY_RESOURCE;
        }
        themeId = fixThemeId(themeId);
        if (!name.endsWith(".css")) {
            name = name + ".css";
        }
        if (name.charAt(0) != '/') {
            name = '/' + name;
        }
        return resourcesCache.getUnchecked(new ResourceKey(themeId, RES_TYPE_STYLE, name));
    }

    @NotNull
    public ResourceData getImage(String themeId, String name) {
        if (StringUtils.isBlank(name)) {
            return EMPTY_RESOURCE;
        }
        themeId = fixThemeId(themeId);
        return resourcesCache.getUnchecked(new ResourceKey(themeId, RES_TYPE_IMAGE, name));
    }

    private String fixThemeId(String themeId) {
        if (StringUtils.isBlank(themeId)) {
            themeId = "citeck-theme-default";
        } else if (themeId.equals(NULL_THEME_ID) || themeId.equals(ACTIVE_THEME_ID)) {
            themeId = getActiveTheme();
        }
        return themeId;
    }

    private ResourceData getInhResourceImpl(ResourceKey key) {

        ResourceData result = getResourceImpl(key);

        if (DEFAULT_THEME_ID.equals(key.getThemeId())
                || (result.getData() != null && result.getData().length > 0)) {

            return result;
        }

        String parentThemeId;
        String parentRef = themeRepo.getParentRef(key.getThemeId()).orElse(null);
        if (StringUtils.isBlank(parentRef)) {
            parentThemeId = DEFAULT_THEME_ID;
        } else {
            parentThemeId = EntityRef.valueOf(parentRef).getLocalId();
        }

        return getInhResourceImpl(new ResourceKey(parentThemeId, key.type, key.path));
    }

    @NotNull
    private ResourceData getResourceImpl(ResourceKey key) {

        if (StringUtils.isBlank(key.getPath()) || StringUtils.isBlank(key.getThemeId())) {
            return EMPTY_RESOURCE;
        }

        ThemeDto dto = toDto(themeRepo.findFirstByExtId(key.getThemeId()).orElse(null));
        if (dto == null) {
            return EMPTY_RESOURCE;
        }

        String path = key.getPath();
        if (key.getType().equals("image")) {
            path = dto.getImages().get(path);
        }
        if (StringUtils.isBlank(path)) {
            return EMPTY_RESOURCE;
        }

        byte[] data;
        String fileName;

        if (path.startsWith("/")) {

            data = dto.getResources().get(path);
            fileName = path.substring(path.lastIndexOf('/') + 1);

        } else if (path.startsWith(ICON_REF_PREFIX)) {

            String localId = path.replaceFirst(ICON_REF_PREFIX, "");
            IconDto iconDto = iconService.findById(localId).orElse(null);
            if (iconDto == null) {
                return EMPTY_RESOURCE;
            }

            data = iconDto.getByteData();
            fileName = iconDto.getId();

            MimeType iconMimetype = iconDto.getMimetype();
            if (!fileName.contains(".") && iconMimetype != null) {
                if (MimeTypeUtils.IMAGE_JPEG.equals(iconMimetype)) {
                    fileName += ".jpg";
                } else if ("image/x-icon".equals(iconMimetype.getType())) {
                    fileName += ".ico";
                } else {
                    fileName += ".png";
                }
            }
        } else {
            log.error("Unsupported resource path: '" + path + "'");
            return EMPTY_RESOURCE;
        }

        return new ResourceData(StringUtils.defaultString(fileName), data != null ? data : new byte[0]);
    }

    public String getCacheKey() {
        return themeRepo.getLastModifiedTime()
            .map(Instant::toEpochMilli)
            .orElse(0L) + "-" + themeRepo.count() + "-" + getActiveTheme();
    }

    @Nullable
    public ThemeDto getTheme(String id) {
        return toDto(themeRepo.findFirstByExtId(id).orElse(null));
    }

    private ThemeDto toDto(ThemeEntity entity) {

        if (entity == null) {
            return null;
        }

        ThemeDto dto = new ThemeDto();

        dto.setId(entity.getExtId());
        dto.setImages(Json.getMapper().readMap(entity.getImages(), String.class, String.class));
        dto.setName(Json.getMapper().read(entity.getName(), MLText.class));
        dto.setParentRef(EntityRef.valueOf(entity.getParentRef()));

        Map<String, byte[]> resources = new HashMap<>();
        dto.setResources(resources);

        byte[] resourcesData = entity.getResources();
        if (resourcesData == null) {
            return dto;
        }
        EcosMemDir stylesDir = ZipUtils.extractZip(resourcesData);
        stylesDir.findFiles().forEach(f -> resources.put(f.getPath().toString().replace("\\", "/"), f.readAsBytes()));

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
        entity.setParentRef(EntityRef.toString(dto.getParentRef()));

        Map<String, byte[]> resources = dto.getResources();
        if (resources == null) {
            entity.setResources(null);
        } else {
            EcosMemDir resourcesDir = new EcosMemDir();
            resources.forEach(resourcesDir::createFile);
            entity.setResources(ZipUtils.writeZipAsBytes(resourcesDir));
        }

        return entity;
    }

    @Data
    @AllArgsConstructor
    private static final class ResourceKey {
        private String themeId;
        private String type;
        private String path;
    }
}
