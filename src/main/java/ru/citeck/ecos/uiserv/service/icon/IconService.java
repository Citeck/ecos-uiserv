package ru.citeck.ecos.uiserv.service.icon;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.uiserv.domain.IconEntity;
import ru.citeck.ecos.uiserv.repository.IconRepository;
import ru.citeck.ecos.uiserv.service.icon.dto.IconDto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    public List<IconDto> findAllByType(String type) {
        return iconRepository.findAllByType(type)
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private IconDto mapToDto(IconEntity entity) {
        IconDto dto = new IconDto();

        dto.setId(entity.getExtId());
        dto.setType(entity.getType());
        dto.setFormat(entity.getFormat());

        dto.setData(bytesToStringByType(entity.getData(), entity.getType()));
        dto.setModified(entity.getLastModifiedDate());

        return dto;
    }

    private String bytesToStringByType(byte[] data, String type) {
        switch (type) {
            case "fa":
                return new String(data, StandardCharsets.UTF_8);
            case "img":
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

        String type = dto.getType();
        entity.setType(type);
        entity.setFormat(dto.getFormat());
        entity.setData(stringToBytesByType(dto.getData(), type));

        return entity;
    }

    private byte[] stringToBytesByType(String data, String type) {
        switch (type) {
            case "fa":
                return data.getBytes(StandardCharsets.UTF_8);
            case "img":
                return Base64.getDecoder().decode(data);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }
}
