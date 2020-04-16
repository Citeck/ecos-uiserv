package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.service.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.DashboardEntity;
import ru.citeck.ecos.uiserv.repository.DashboardRepository;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository repo;
    private final RecordsService recordsService;

    private Consumer<DashboardDto> changeListener;

    public List<DashboardDto> getAllDashboards() {
        return repo.findAll()
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public void addChangeListener(Consumer<DashboardDto> changeListener) {
        this.changeListener = changeListener;
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
        DashboardDto result = mapToDto(repo.save(entity));
        changeListener.accept(result);
        return result;
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

            ExpandedTypeMeta typeMeta = recordsService.getMeta(type, ExpandedTypeMeta.class);
            if (typeMeta == null) {
                return Optional.empty();
            }
            for (ParentMeta parent : typeMeta.getParents()) {
                if (!Objects.equals(parent.inhDashboardType, typeMeta.inhDashboardType)) {
                    return Optional.empty();
                }
                dashboards = findDashboardsByType(parent.id, authorities, includeForAll);
                if (!dashboards.isEmpty()) {
                    break;
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
        dto.setConfig(Json.getMapper().read(entity.getConfig(), ObjectData.class));
        dto.setPriority(entity.getPriority());
        dto.setTypeRef(RecordRef.valueOf(entity.getTypeRef()));

        return dto;
    }

    private DashboardEntity mapToEntity(DashboardDto dto) {

        Optional<DashboardEntity> optEntity;
        String authority = StringUtils.isBlank(dto.getAuthority()) ? null : dto.getAuthority();
        if (authority == null) {
            optEntity = repo.findByTypeRefForAll(dto.getTypeRef().toString());
        } else {
            optEntity = repo.findByAuthorityAndTypeRef(authority, dto.getTypeRef().toString());
        }

        DashboardEntity entity = optEntity.orElseGet(() -> {
            DashboardEntity newDashboard = new DashboardEntity();
            newDashboard.setExtId(StringUtils.isBlank(dto.getId()) ? UUID.randomUUID().toString() : dto.getId());
            newDashboard.setAuthority(authority);
            newDashboard.setTypeRef(dto.getTypeRef().toString());
            return newDashboard;
        });

        if (dto.getConfig() != null && dto.getConfig().size() > 0) {
            entity.setConfig(Json.getMapper().toBytes(dto.getConfig()));
        }

        return entity;
    }

    @Data
    private static class ExpandedTypeMeta {
        private List<ParentMeta> parents;
        private String inhDashboardType;
    }

    @Data
    public static class ParentMeta {
        private String id;
        private String inhDashboardType;
    }
}
