package ru.citeck.ecos.uiserv.service.journal.link;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.uiserv.domain.UserConfigurationDto;
import ru.citeck.ecos.uiserv.domain.UserConfigurationEntity;
import ru.citeck.ecos.uiserv.repository.UserConfigurationsRepository;

import java.util.UUID;

@Service
@Transactional
public class UserConfigurationsService {
    private static final int LIMIT = 1000;

    private UserConfigurationsRepository userConfigurationsRepository;

    public UserConfigurationsService(UserConfigurationsRepository userConfigurationsRepository) {
        this.userConfigurationsRepository = userConfigurationsRepository;
    }

    public UserConfigurationDto save(UserConfigurationDto userConfigurationDto) {
        UserConfigurationEntity entity = userConfigurationsRepository.save(mapToEntity(userConfigurationDto));

        removeLatestIfExceedsLimit(userConfigurationDto.getUserName());

        return mapToDto(entity);
    }

    private void removeLatestIfExceedsLimit(String userName) {
        int exceededCount = userConfigurationsRepository.countByUserName(userName) - LIMIT;

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
        dto.setData(new DataValue(entity.getData()));

        return dto;
    }

    private UserConfigurationEntity mapToEntity(UserConfigurationDto dto) {
        UserConfigurationEntity entity = new UserConfigurationEntity();

        entity.setExternalId(UUID.randomUUID().toString());
        entity.setUserName(dto.getUserName());
        entity.setCreationTime(dto.getCreationTime());
        entity.setData(dto.getData().asText());

        return entity;
    }
}
