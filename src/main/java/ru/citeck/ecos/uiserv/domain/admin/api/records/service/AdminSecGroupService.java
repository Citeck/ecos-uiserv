package ru.citeck.ecos.uiserv.domain.admin.api.records.service;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.admin.api.records.dto.AdminSecGroupDto;
import ru.citeck.ecos.uiserv.domain.admin.api.records.dto.AdminSectionDto;
import ru.citeck.ecos.uiserv.domain.admin.api.records.repo.AdminSecGroupEntity;
import ru.citeck.ecos.uiserv.domain.admin.api.records.repo.AdminSecGroupRepository;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalServiceImpl;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSecGroupService {

    private static final String JOURNAL_NAME = "journal";

    private final AdminSecGroupRepository adminSecGroupRepository;
    private final JournalServiceImpl journalService;

    public AdminSecGroupDto save(AdminSecGroupDto dto) {
        AdminSecGroupEntity entity = new AdminSecGroupEntity();

        entity.setExternalId(dto.getId());
        entity.setName(dto.getName().get());
        entity.setOrder(dto.getOrder());

        val listSection = new ArrayList<AdminSectionDto>();

        dto.getSections().forEach(i -> {
            val admSectBuilder = new AdminSectionDto.Builder()
                .withType(i.getType())
                .withConfig(i.getConfig());

            if (MLText.EMPTY.equals(i.getName())) {
                if (JOURNAL_NAME.equals(i.getType())) {
                    val journal = journalService.getJournalById(i.getType());
                    admSectBuilder.withName(journal.getJournalDef().getName());
                }
            } else {
                admSectBuilder.withName(i.getName());
            }

            listSection.add(admSectBuilder.build());
        });
        entity.setSections(Json.getMapper().toString(listSection));
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
