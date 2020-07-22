package ru.citeck.ecos.uiserv.domain.userconfig.service;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.userconfig.repo.UserConfigurationEntity;
import ru.citeck.ecos.uiserv.domain.userconfig.dto.UserConfigurationDto;
import ru.citeck.ecos.uiserv.domain.userconfig.repo.UserConfigurationsRepository;
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class UserConfigurationsService {

    @Value("${max-user-configurations-persisted}")
    private Integer limit;

    private final UserConfigurationsRepository userConfigurationsRepository;

    public UserConfigurationsService(UserConfigurationsRepository userConfigurationsRepository) {
        this.userConfigurationsRepository = userConfigurationsRepository;
    }

    public UserConfigurationDto save(UserConfigurationDto userConfigurationDto) {
        if (userConfigurationDto == null || userConfigurationDto.getData() == null) {
            throw new IllegalArgumentException("User configuration and data cannot be null");
        }

        UserConfigurationEntity entity = userConfigurationsRepository.save(mapToEntity(userConfigurationDto));

        removeLatestIfExceedsLimit(userConfigurationDto.getUserName());

        return mapToDto(entity);
    }

    private void removeLatestIfExceedsLimit(String userName) {
        int exceededCount = userConfigurationsRepository.countByUserName(userName) - limit;

        for (int i = 0; i < exceededCount; i++) {
            removeLatest(userName);
        }
    }

    private void removeLatest(String userName) {
        UserConfigurationEntity latest = userConfigurationsRepository.findTopByUserNameOrderByCreationTimeAsc(userName);
        userConfigurationsRepository.delete(latest);
    }

    @Nullable
    public UserConfigurationDto findByExternalId(String externalId) {
        UserConfigurationEntity entity = userConfigurationsRepository.findByExternalId(externalId);
        if (entity == null) {
            return null;
        }
        return mapToDto(entity);
    }

    private UserConfigurationDto mapToDto(UserConfigurationEntity entity) {
        UserConfigurationDto dto = new UserConfigurationDto();

        dto.setId(entity.getExternalId());
        dto.setUserName(entity.getUserName());
        dto.setCreationTime(entity.getCreationTime());
        dto.setData(ObjectData.create(entity.getData()));

        return dto;
    }

    private UserConfigurationEntity mapToEntity(UserConfigurationDto dto) {
        UserConfigurationEntity entity = new UserConfigurationEntity();

        entity.setExternalId(UUID.randomUUID().toString());
        entity.setUserName(SecurityUtils.getCurrentUserLoginFromRequestContext());
        entity.setCreationTime(Instant.now());
        entity.setData(Json.getMapper().toString(dto.getData()));

        return entity;
    }
}
