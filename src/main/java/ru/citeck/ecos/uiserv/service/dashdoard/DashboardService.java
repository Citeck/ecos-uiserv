package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.objdata.DataValue;
import ru.citeck.ecos.records2.objdata.ObjectData;
import ru.citeck.ecos.records2.utils.json.JsonUtils;
import ru.citeck.ecos.uiserv.domain.DashboardDto;
import ru.citeck.ecos.uiserv.domain.DashboardEntity;
import ru.citeck.ecos.uiserv.repository.DashboardRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository repo;
    private final RecordsService recordsService;

    public List<DashboardDto> getAllDashboards() {
        return repo.findAll()
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public Optional<DashboardDto> getDashboardById(String id) {
        return repo.findByExtId(id).map(this::mapToDto);
    }

    public Optional<DashboardDto> getForAuthority(RecordRef type,
                                                  String user,
                                                  boolean expandType,
                                                  boolean includeForAll) {

        return getEntityForUser(type, user, expandType, includeForAll).map(this::mapToDto);
    }

    public DashboardDto saveDashboard(DashboardDto dashboard) {

        DashboardEntity entity = mapToEntity(dashboard);
        return mapToDto(repo.save(entity));
    }

    public void removeDashboard(String id) {
        repo.findByExtId(id).ifPresent(repo::delete);
    }

    private Optional<DashboardEntity> getEntityForUser(RecordRef type,
                                                       String user,
                                                       boolean expandType,
                                                       boolean includeForAll) {

        List<String> authorities = StringUtils.isNotBlank(user) ?
            Collections.singletonList(user) : Collections.emptyList();

        List<DashboardEntity> dashboards = findDashboardsByType(type.toString(), authorities, includeForAll);

        if (dashboards.isEmpty() && expandType) {

            DataValue parents = recordsService.getAttribute(type, "parents[]?id");
            for (DataValue parent : parents) {
                if (parent.isTextual()) {
                    dashboards = findDashboardsByType(parent.asText(), authorities, includeForAll);
                    if (!dashboards.isEmpty()) {
                        break;
                    }
                }
            }
        }

        return dashboards.stream().findFirst();
    }

    private List<DashboardEntity> findDashboardsByType(String type, List<String> authorities, boolean includeForAll) {

        if (!authorities.isEmpty()) {

            PageRequest page = PageRequest.of(0, 1);
            List<DashboardEntity> dashboards = repo.findForAuthorities(type, authorities, page);
            if (dashboards.isEmpty() && includeForAll) {
                dashboards = repo.findByTypeRefForAll(type)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
            }
            return dashboards;

        } else {

            Optional<DashboardEntity> entity = repo.findByTypeRefForAll(type);
            return entity.map(Collections::singletonList).orElse(Collections.emptyList());
        }
    }

    private DashboardDto mapToDto(DashboardEntity entity) {

        DashboardDto dto = new DashboardDto();

        dto.setId(entity.getExtId());
        dto.setAuthority(entity.getAuthority());
        dto.setConfig(JsonUtils.read(entity.getConfig(), ObjectData.class));
        dto.setPriority(entity.getPriority());
        dto.setTypeRef(RecordRef.valueOf(entity.getTypeRef()));

        return dto;
    }

    private DashboardEntity mapToEntity(DashboardDto dto) {

        DashboardEntity entity = repo.findByExtId(dto.getId())
            .orElseGet(DashboardEntity::new);

        if (entity.getId() != null &&
            (!Objects.equals(entity.getAuthority(), dto.getAuthority())
                || !Objects.equals(entity.getTypeRef(), String.valueOf(dto.getTypeRef())))) {

            throw new RuntimeException("Dashboard collision. Entity: " + entity.getId()
                + " " + entity.getAuthority()  + " "+ entity.getTypeRef() + " "
                + " dto: " + dto.getAuthority() + " " + dto.getTypeRef());
        }

        if (dto.getConfig() != null && dto.getConfig().size() > 0) {
            entity.setConfig(JsonUtils.toBytes(dto.getConfig()));
        }

        entity.setExtId(dto.getId());
        entity.setAuthority(dto.getAuthority());
        entity.setTypeRef(dto.getTypeRef().toString());

        return entity;
    }
}
