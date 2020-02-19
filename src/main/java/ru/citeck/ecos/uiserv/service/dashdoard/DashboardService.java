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

    public Optional<DashboardDto> getDashboard(RecordRef type, String user) {
        return getDashboardEntity(type, user).map(this::mapToDto);
    }

    public DashboardDto saveDashboard(DashboardDto dashboard) {

        DashboardEntity entity = mapToEntity(dashboard);
        return mapToDto(repo.save(entity));
    }

    public void removeDashboard(String id) {
        repo.findByExtId(id).ifPresent(repo::delete);
    }

    private Optional<DashboardEntity> getDashboardEntity(RecordRef type, String user) {

        List<String> authorities = StringUtils.isNotBlank(user) ?
            Collections.singletonList(user) : Collections.emptyList();

        List<DashboardEntity> dashboards = findDashboardsByType(type.toString(), authorities);

        if (dashboards.isEmpty()) {

            DataValue parents = recordsService.getAttribute(type, "parents[]?id");
            for (DataValue parent : parents) {
                if (parent.isTextual()) {
                    dashboards = findDashboardsByType(parent.asText(), authorities);
                    if (!dashboards.isEmpty()) {
                        break;
                    }
                }
            }
        }

        return dashboards.stream().findFirst();
    }

    private List<DashboardEntity> findDashboardsByType(String type, List<String> authorities) {

        if (!authorities.isEmpty()) {

            PageRequest page = PageRequest.of(0, 1);
            return repo.findForAuthorities(type, authorities, page);

        } else {

            Optional<DashboardEntity> entity = repo.findByTypeRef(type);
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

        if (dto.getConfig() != null && dto.getConfig().size() > 0) {
            entity.setConfig(JsonUtils.toBytes(dto.getConfig()));
        }

        entity.setExtId(dto.getId());
        entity.setAuthority(dto.getAuthority());
        entity.setTypeRef(dto.getTypeRef().toString());

        return entity;
    }
}
