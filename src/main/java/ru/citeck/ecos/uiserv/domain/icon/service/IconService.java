package ru.citeck.ecos.uiserv.domain.icon.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.app.common.perms.UiServSystemArtifactPerms;
import ru.citeck.ecos.uiserv.domain.icon.api.records.IconRecords;
import ru.citeck.ecos.uiserv.domain.icon.repo.IconEntity;
import ru.citeck.ecos.uiserv.domain.icon.repo.IconRepository;
import ru.citeck.ecos.uiserv.domain.icon.dto.IconDto;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IconService {
    private final IconRepository iconRepository;
    private final UiServSystemArtifactPerms perms;
    private final JpaSearchConverterFactory jpaSearchConverterFactory;

    private JpaSearchConverter<IconEntity> searchConv;

    @PostConstruct
    public void init() {
        searchConv = jpaSearchConverterFactory.createConverter(IconEntity.class)
            // override default "type" → "" exclusion, since IconEntity.type is a real column
            .withAttMapping("type", "type")
            .build();
    }

    public Optional<IconDto> findById(String id) {
        if (StringUtils.isBlank(id)) {
            return Optional.empty();
        }
        return iconRepository.findByExtId(id)
            .map(this::mapToDto);
    }

    public void deleteById(String id) {
        perms.checkWrite(EntityRef.create(AppName.UISERV, IconRecords.ID, id));

        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Id parameter is mandatory for icon deletion");
        }
        iconRepository.findByExtId(id).ifPresent(iconRepository::delete);
    }

    public IconDto save(IconDto iconDto) {
        perms.checkWrite(EntityRef.create(AppName.UISERV, IconRecords.ID, iconDto.getId()));

        if (StringUtils.isBlank(iconDto.getId())) {
            iconDto.setId(UUID.randomUUID().toString());
        }
        IconEntity saved = iconRepository.save(mapToEntity(iconDto));
        return mapToDto(saved);
    }

    public List<IconDto> findAll(Predicate predicate, int max, int skip, List<SortBy> sort) {
        return searchConv.findAll(iconRepository, predicate, max, skip, sort)
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public long getCount(Predicate predicate) {
        return searchConv.getCount(iconRepository, predicate);
    }

    public String getCacheKey() {
        return iconRepository.getLastModifiedTime()
            .map(Instant::toEpochMilli)
            .orElse(0L) + "-" + iconRepository.count();
    }

    private IconDto mapToDto(IconEntity entity) {
        IconDto dto = new IconDto();

        dto.setId(entity.getExtId());
        dto.setType(entity.getType());
        dto.setFamily(entity.getFamily());
        dto.setConfig(Json.getMapper().read(entity.getConfig(), ObjectData.class));
        dto.setData(entity.getData());
        dto.setModified(entity.getLastModifiedDate());

        String mimeTypeStr = entity.getMimeType();
        if (mimeTypeStr != null) {
            dto.setMimetype(MimeType.valueOf(mimeTypeStr));
        }

        return dto;
    }

    private IconEntity mapToEntity(IconDto dto) {

        String family = dto.getFamily() != null ? dto.getFamily() : "";

        IconEntity entity;
        String extId = dto.getId();
        if (StringUtils.isNotBlank(extId)) {
            entity = iconRepository.findByExtId(extId).orElseGet(() -> {
                IconEntity newEntity = new IconEntity();
                newEntity.setExtId(extId);
                return newEntity;
            });
        } else {
            entity = new IconEntity();
            entity.setExtId(UUID.randomUUID().toString());
        }

        String type = dto.getType();
        entity.setType(type);
        entity.setConfig(Json.getMapper().toString(dto.getConfig()));
        entity.setFamily(family);
        entity.setData(dto.getByteData());

        MimeType mimeType = dto.getMimetype();
        if (mimeType != null) {
            entity.setMimeType(mimeType.toString());
        }

        return entity;
    }
}
