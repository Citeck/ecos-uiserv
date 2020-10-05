package ru.citeck.ecos.uiserv.domain.journal.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsEntity;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalSettingsService {

    private final JournalSettingsRepository repo;

    public JournalSettingsDto save(JournalSettingsDto settings) {
        JournalSettingsEntity entity = toEntity(settings);
        entity = repo.save(entity);
        return toDto(entity);
    }

    @Nullable
    public JournalSettingsDto getById(String id) {
        JournalSettingsEntity entity = repo.findByExtId(id);
        if (entity == null) {
            return null;
        }
        return toDto(entity);
    }

    public boolean delete(String id) {
        JournalSettingsEntity entity = repo.findByExtId(id);
        if (entity != null) {
            repo.delete(entity);
            return true;
        }
        return false;
    }

    public List<JournalSettingsDto> getSettings(String authority, String journalId) {

        List<JournalSettingsEntity> configs = repo.findAllByAuthorityAndJournalId(authority, journalId);

        return configs.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    private JournalSettingsEntity toEntity(JournalSettingsDto dto) {

        JournalSettingsEntity settingsEntity;

        if (StringUtils.isBlank(dto.getId())) {
            settingsEntity = new JournalSettingsEntity();
            settingsEntity.setExtId(UUID.randomUUID().toString());
        } else {
            settingsEntity = repo.findByExtId(dto.getId());
            if (settingsEntity == null) {
                settingsEntity = new JournalSettingsEntity();
                settingsEntity.setExtId(dto.getId());
            }
        }

        settingsEntity.setName(dto.getName());
        settingsEntity.setSettings(Json.getMapper().toString(dto.getSettings()));
        settingsEntity.setJournalId(dto.getJournalId());
        settingsEntity.setAuthority(dto.getAuthority());

        return settingsEntity;
    }

    private JournalSettingsDto toDto(JournalSettingsEntity entity) {

        JournalSettingsDto dto = new JournalSettingsDto();
        dto.setId(entity.getExtId());
        dto.setAuthority(entity.getAuthority());
        dto.setJournalId(entity.getJournalId());
        dto.setSettings(Json.getMapper().read(entity.getSettings(), ObjectData.class));
        dto.setName(entity.getName());

        return dto;
    }
}
