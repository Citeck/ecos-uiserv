package ru.citeck.ecos.uiserv.domain.dashdoard.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.app.application.constants.AppConstants;
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.repo.DashboardEntity;
import ru.citeck.ecos.uiserv.domain.dashdoard.repo.DashboardRepository;

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

    public Optional<DashboardDto> getForAuthority(RecordRef recordRef,
                                                  RecordRef type,
                                                  String user,
                                                  boolean expandType,
                                                  boolean includeForAll) {

        return getEntityForUser(recordRef, type, user, expandType, includeForAll).map(this::mapToDto);
    }

    public DashboardDto saveDashboard(DashboardDto dashboard) {
        updateAuthority(dashboard);
        DashboardEntity entity = mapToEntity(dashboard);
        DashboardDto result = mapToDto(repo.save(entity));
        changeListener.accept(result);
        return result;
    }

    private void updateAuthority(DashboardDto dashboard) {

        String currentUserLogin = getCurrentUserLogin();
        RecordRef userRef = RecordRef.valueOf("alfresco/people@" + currentUserLogin);
        String authority = dashboard.getAuthority();

        if (AppConstants.SYSTEM_ACCOUNT.equals(currentUserLogin) || currentUserLogin.equals(authority)) {
            return;
        }

        boolean isAdmin = recordsService.getAtt(userRef, "isAdmin?bool").asBoolean(false);
        if (isAdmin) {
            return;
        }

        if (StringUtils.isBlank(authority)) {
            dashboard.setAuthority(currentUserLogin);
            return;
        }
        throw new AccessDeniedException(
            "User '" + currentUserLogin + "' can only change his dashboard. " +
            "But tried to change dashboard for authority '" + authority + "'"
        );
    }

    @NotNull
    private String getCurrentUserLogin() {
        String currentUserLogin = SecurityUtils.getCurrentUserLoginFromRequestContext();
        if (currentUserLogin == null) {
            throw new RuntimeException("User is not authenticated");
        }
        return currentUserLogin;
    }

    public void removeDashboard(String id) {
        repo.findByExtId(id).ifPresent(repo::delete);
    }

    private Optional<DashboardEntity> getEntityForUser(RecordRef recordRef,
                                                       RecordRef type,
                                                       String user,
                                                       boolean expandType,
                                                       boolean includeForAll) {

        List<String> authorities = StringUtils.isNotBlank(user) ?
            Collections.singletonList(user) : Collections.emptyList();

        authorities = authorities.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());

        List<DashboardEntity> dashboards;
        if (!RecordRef.isEmpty(recordRef)) {

            if (recordRef.getAppName().isEmpty()) {
                recordRef = recordRef.addAppName("alfresco");
            }

            dashboards = findDashboardsByRecordRef(recordRef.toString(), authorities, includeForAll);
            if (!dashboards.isEmpty()) {
                return dashboards.stream().findFirst();
            }
        }

        dashboards = findDashboardsByType(type.toString(), authorities, includeForAll);

        if (dashboards.isEmpty() && expandType) {

            ExpandedTypeMeta typeMeta = recordsService.getMeta(type, ExpandedTypeMeta.class);
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

    private List<DashboardEntity> findDashboardsByRecordRef(String recordRef,
                                                            List<String> authorities,
                                                            boolean includeForAll) {

        if (!authorities.isEmpty()) {

            PageRequest page = PageRequest.of(0, 1);
            List<DashboardEntity> dashboards = repo.findForRefAndAuthorities(recordRef, authorities, page);
            if (dashboards.isEmpty() && includeForAll) {
                dashboards = repo.findByRecordRefForAll(recordRef)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
            }
            return dashboards;

        } else {

            Optional<DashboardEntity> entity = repo.findByRecordRefForAll(recordRef);
            return entity.map(Collections::singletonList).orElse(Collections.emptyList());
        }
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
        dto.setAppliedToRef(RecordRef.valueOf(entity.getAppliedToRef()));

        return dto;
    }

    private DashboardEntity mapToEntity(DashboardDto dto) {

        Optional<DashboardEntity> optEntity;
        String authority = StringUtils.isBlank(dto.getAuthority()) ? null : dto.getAuthority();
        RecordRef recordRef = dto.getAppliedToRef();

        if (RecordRef.isNotEmpty(recordRef) && recordRef.getAppName().isEmpty()) {
            recordRef = recordRef.addAppName("alfresco");
        }

        if (RecordRef.isEmpty(dto.getTypeRef()) && RecordRef.isEmpty(dto.getAppliedToRef())) {
            throw new IllegalArgumentException("One of typeRef or appliedToRef should be specified");
        }

        if (authority == null) {
            if (RecordRef.isEmpty(recordRef)) {
                optEntity = repo.findByTypeRefForAll(dto.getTypeRef().toString());
            } else {
                optEntity = repo.findByRecordRefForAll(recordRef.toString());
            }
        } else {
            if (RecordRef.isEmpty(recordRef)) {
                optEntity = repo.findByAuthorityAndTypeRef(authority, dto.getTypeRef().toString());
            } else {
                optEntity = repo.findByAuthorityAndAppliedToRef(authority, recordRef.toString());
            }
        }

        RecordRef finalRecordRef = recordRef;

        DashboardEntity entity = optEntity.orElseGet(() -> {

            DashboardEntity newDashboard = new DashboardEntity();

            String extId = dto.getId();
            if (StringUtils.isNotBlank(extId)) {
                if (repo.findByExtId(extId).isPresent()) {
                    extId = null;
                }
            }
            if (StringUtils.isBlank(extId)) {
                extId = UUID.randomUUID().toString();
            }

            newDashboard.setExtId(extId);
            newDashboard.setAuthority(authority);
            newDashboard.setTypeRef(RecordRef.toString(dto.getTypeRef()));
            if (RecordRef.isNotEmpty(finalRecordRef)) {
                newDashboard.setAppliedToRef(finalRecordRef.toString());
            }
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
        @AttName("?id")
        private String id;
        private String inhDashboardType;
    }
}
