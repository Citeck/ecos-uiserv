package ru.citeck.ecos.uiserv.service.icon;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.uiserv.domain.IconEntity;
import ru.citeck.ecos.uiserv.repository.IconRepository;
import ru.citeck.ecos.uiserv.service.icon.dto.IconDto;
import ru.citeck.ecos.uiserv.service.icon.dto.IconType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IconService {
    private final IconRepository iconRepository;

    public IconDto save(IconDto iconDto) {
        IconEntity saved = iconRepository.save(mapToEntity(iconDto));

        return mapToDto(saved);
    }

    public List<IconDto> findAllByType(IconType type) {
        return iconRepository.findAllByType(type.getTypeString())
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private IconDto mapToDto(IconEntity entity) {
        IconDto dto = new IconDto();

        dto.setId(entity.getExtId());

        IconType type = IconType.byTypeString(entity.getType());
        if (type == null) {
            throw new IllegalStateException("Unsupported type in repository: " + entity.getType());
        }
        dto.setType(type);
        if (IconType.IMG.equals(type)) {
            dto.setFormat(entity.getFormat());
        }
        dto.setData(bytesToStringByType(entity.getData(), type));
        dto.setModified(entity.getLastModifiedDate());

        return dto;
    }

    private String bytesToStringByType(byte[] data, IconType type) {
        switch (type) {
            case FA:
                return new String(data, StandardCharsets.UTF_8);
            case IMG:
                return Base64.getEncoder().encodeToString(data);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private IconEntity mapToEntity(IconDto dto) {
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

        IconType type = dto.getType();
        entity.setType(type.getTypeString());
        if (IconType.IMG.equals(type)) {
            entity.setFormat(dto.getFormat());
        }
        entity.setData(stringToBytesByType(dto.getData(), type));

        return entity;
    }

    private byte[] stringToBytesByType(String data, IconType type) {
        switch (type) {
            case FA:
                return data.getBytes(StandardCharsets.UTF_8);
            case IMG:
                return Base64.getDecoder().decode(data);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }
}
