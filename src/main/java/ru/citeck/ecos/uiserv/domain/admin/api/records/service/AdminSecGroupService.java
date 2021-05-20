package ru.citeck.ecos.uiserv.domain.admin.api.records.service;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.admin.api.records.dto.AdminSecGroupDto;
import ru.citeck.ecos.uiserv.domain.admin.api.records.dto.AdminSectionDto;
import ru.citeck.ecos.uiserv.domain.admin.api.records.repo.AdminSecGroupEntity;
import ru.citeck.ecos.uiserv.domain.admin.api.records.repo.AdminSecGroupRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSecGroupService {

    private static final String JOURNAL_NAME = "journal";

    private final AdminSecGroupRepository adminSecGroupRepository;

    @Transactional
    public AdminSecGroupDto save(AdminSecGroupDto dto) {
        val entity = adminSecGroupRepository.findByExternalId(dto.getId())
            .orElse(new AdminSecGroupEntity());

        entity.setExternalId(dto.getId());
        entity.setName(dto.getName().get());
        entity.setOrder(dto.getOrder());
        entity.setSections(Json.getMapper().toString(dto.getSections()));

        return mapToDto(adminSecGroupRepository.save(entity));
    }

    public List<AdminSecGroupDto> getAll() {
        return adminSecGroupRepository.findAll().stream()
            .map(this::mapToDto)
            .sorted(Comparator.comparing(AdminSecGroupDto::getOrder))
            .collect(Collectors.toList());
    }

    private AdminSecGroupDto mapToDto(AdminSecGroupEntity entity) {
        return AdminSecGroupDto.create()
            .withId(entity.getExternalId())
            .withName(new MLText(entity.getName()))
            .withOrder(entity.getOrder())
            .withSections(DataValue.create(entity.getSections()).asList(AdminSectionDto.class))
            .build();
    }

}
