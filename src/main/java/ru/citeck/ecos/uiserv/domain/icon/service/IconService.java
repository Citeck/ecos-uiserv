package ru.citeck.ecos.uiserv.domain.icon.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.icon.repo.IconEntity;
import ru.citeck.ecos.uiserv.domain.icon.repo.IconRepository;
import ru.citeck.ecos.uiserv.domain.icon.dto.IconDto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IconService {
    private final IconRepository iconRepository;

    public List<IconDto> findAll() {
        return iconRepository.findAll().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public Optional<IconDto> findById(String id) {
        if (StringUtils.isBlank(id)) {
            return Optional.empty();
        }
        return iconRepository.findByExtId(id)
            .map(this::mapToDto);
    }

    public void deleteById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Id parameter is mandatory for icon deletion");
        }
        iconRepository.findByExtId(id).ifPresent(iconRepository::delete);
    }

    public IconDto save(IconDto iconDto) {
        if (StringUtils.isBlank(iconDto.getId())) {
            iconDto.setId(UUID.randomUUID().toString());
        }
        IconEntity saved = iconRepository.save(mapToEntity(iconDto));
        return mapToDto(saved);
    }

    public List<IconDto> findAllByFamilyAndType(String family, String type) {
        return iconRepository.findAllByFamilyAndType(family, type)
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public List<IconDto> findAllByFamily(String family) {
        return iconRepository.findAllByFamily(family)
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
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
