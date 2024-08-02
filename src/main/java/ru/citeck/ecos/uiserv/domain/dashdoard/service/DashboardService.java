package ru.citeck.ecos.uiserv.domain.dashdoard.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.repo.DashboardEntity;
import ru.citeck.ecos.uiserv.domain.dashdoard.repo.DashboardRepository;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository repo;
    private final RecordsService recordsService;
    private final JpaSearchConverterFactory jpaSearchConverterFactory;

    private JpaSearchConverter<DashboardEntity> searchConv;

    private final List<BiConsumer<DashboardDto, DashboardDto>> changeListeners = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        searchConv = jpaSearchConverterFactory.createConverter(DashboardEntity.class).build();
    }

    public long getCount(Predicate predicate) {
        return searchConv.getCount(repo, predicate);
    }

    public List<DashboardDto> findAll(Predicate predicate, int max, int skip, List<SortBy> sort) {
        return searchConv.findAll(repo, predicate, max, skip, sort).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public List<DashboardDto> getAllDashboards() {
        return repo.findAll()
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public void addChangeListener(BiConsumer<DashboardDto, DashboardDto> changeListener) {
        changeListeners.add(changeListener);
    }

    public Optional<DashboardDto> getDashboardById(String id) {
        return repo.findByExtId(id).map(this::mapToDto);
    }

    public Optional<DashboardDto> getForAuthority(EntityRef recordRef,
                                                  EntityRef type,
                                                  String user,
                                                  String scope,
                                                  boolean expandType,
                                                  boolean includeForAll) {

        return getEntityForUser(
            recordRef,
            type,
            user,
            StringUtils.defaultString(scope),
            expandType,
            includeForAll
        ).map(this::mapToDto);
    }

    public DashboardDto saveDashboard(DashboardDto dashboard) {
        updateAuthority(dashboard);

        DashboardEntity entityBefore = findEntityForDto(dashboard);
        DashboardDto valueBefore = Optional.ofNullable(entityBefore)
            .map(this::mapToDto)
            .orElse(null);

        DashboardEntity entity = mapToEntity(dashboard, entityBefore);
        DashboardDto result = mapToDto(repo.save(entity));

        for (BiConsumer<DashboardDto, DashboardDto> listener : changeListeners) {
            listener.accept(valueBefore, result);
        }
        return result;
    }

    private void updateAuthority(DashboardDto dashboard) {

        String currentUserLogin = getCurrentUserLogin();
        String authority = dashboard.getAuthority();

        if (AuthContext.isRunAsSystem() || AuthContext.isRunAsAdmin() || currentUserLogin.equals(authority)) {
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
        String currentUserLogin = AuthContext.getCurrentUser();
        if (currentUserLogin.isEmpty()) {
            throw new RuntimeException("User is not authenticated");
        }
        return currentUserLogin;
    }

    public void removeDashboard(String id) {
        repo.findByExtId(id).ifPresent(repo::delete);
    }

    private Optional<DashboardEntity> getEntityForUser(EntityRef recordRef,
                                                       EntityRef type,
                                                       String user,
                                                       @NotNull String scope,
                                                       boolean expandType,
                                                       boolean includeForAll) {

        List<String> authorities = StringUtils.isNotBlank(user) ?
            Collections.singletonList(user) : Collections.emptyList();

        authorities = authorities.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());

        List<DashboardEntity> dashboards;
        if (!EntityRef.isEmpty(recordRef)) {

            if (recordRef.getAppName().isEmpty()) {
                recordRef = recordRef.withAppName(AppName.ALFRESCO);
            }

            dashboards = findDashboardsByRecordRef(recordRef.toString(), authorities, scope, includeForAll);
            if (!dashboards.isEmpty()) {
                return dashboards.stream().findFirst();
            }
        }

        dashboards = findDashboardsByType(type.toString(), authorities, scope, includeForAll);

        if (dashboards.isEmpty() && expandType) {

            ExpandedTypeMeta typeMeta = recordsService.getAtts(type, ExpandedTypeMeta.class);
            for (ParentMeta parent : typeMeta.getParents()) {
                if (!Objects.equals(parent.inhDashboardType, typeMeta.inhDashboardType)) {
                    return Optional.empty();
                }
                dashboards = findDashboardsByType(parent.id, authorities, scope, includeForAll);
                if (!dashboards.isEmpty()) {
                    break;
                }
            }
        }

        return dashboards.stream().findFirst();
    }

    private List<DashboardEntity> findDashboardsByRecordRef(String recordRef,
                                                            List<String> authorities,
                                                            @NotNull String scope,
                                                            boolean includeForAll) {

        if (!authorities.isEmpty()) {

            PageRequest page = PageRequest.of(0, 1);
            List<DashboardEntity> dashboards = repo.findForRefAndAuthorities(recordRef, authorities, scope, page);
            if (dashboards.isEmpty() && includeForAll) {
                dashboards = repo.findByRecordRefForAll(recordRef, scope)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
            }
            return dashboards;

        } else {

            Optional<DashboardEntity> entity = repo.findByRecordRefForAll(recordRef, scope);
            return entity.map(Collections::singletonList).orElse(Collections.emptyList());
        }
    }

    private List<DashboardEntity> findDashboardsByType(String type,
                                                       List<String> authorities,
                                                       @NotNull String scope,
                                                       boolean includeForAll) {

        if (!authorities.isEmpty()) {

            PageRequest page = PageRequest.of(0, 1);
            List<DashboardEntity> dashboards = repo.findForAuthorities(type, authorities, scope, page);
            if (dashboards.isEmpty() && includeForAll) {
                dashboards = repo.findByTypeRefForAll(type, scope)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
            }
            return dashboards;

        } else {

            Optional<DashboardEntity> entity = repo.findByTypeRefForAll(type, scope);
            return entity.map(Collections::singletonList).orElse(Collections.emptyList());
        }
    }

    private DashboardDto mapToDto(DashboardEntity entity) {

        DashboardDto dto = new DashboardDto();

        dto.setId(entity.getExtId());
        dto.setName(Json.getMapper().read(entity.getName(), MLText.class));
        dto.setAuthority(entity.getAuthority());
        dto.setConfig(Json.getMapper().read(entity.getConfig(), ObjectData.class));
        dto.setPriority(entity.getPriority());
        dto.setScope(StringUtils.defaultString(entity.getScope()));
        dto.setTypeRef(EntityRef.valueOf(entity.getTypeRef()));
        dto.setAppliedToRef(EntityRef.valueOf(entity.getAppliedToRef()));

        return dto;
    }

    @Nullable
    private DashboardEntity findEntityForDto(DashboardDto dto) {

        Optional<DashboardEntity> optEntity;
        String authority = getAuthorityFromDto(dto);
        EntityRef recordRef = dto.getAppliedToRef();

        if (EntityRef.isNotEmpty(recordRef) && recordRef.getAppName().isEmpty()) {
            recordRef = recordRef.withAppName(AppName.ALFRESCO);
        }

        if (EntityRef.isEmpty(dto.getTypeRef()) && EntityRef.isEmpty(dto.getAppliedToRef())) {
            optEntity = repo.findByExtId(dto.getId());
        } else {
            String scope = StringUtils.defaultString(dto.getScope());
            if (authority == null) {
                if (EntityRef.isEmpty(recordRef)) {
                    optEntity = repo.findByTypeRefForAll(dto.getTypeRef().toString(), scope);
                } else {
                    optEntity = repo.findByRecordRefForAll(recordRef.toString(), scope);
                }
            } else {
                if (EntityRef.isEmpty(recordRef)) {
                    optEntity = repo.findByAuthorityAndTypeRefAndScope(authority, dto.getTypeRef().toString(), scope);
                } else {
                    optEntity = repo.findByAuthorityAndAppliedToRefAndScope(authority, recordRef.toString(), scope);
                }
            }
        }

        return optEntity.orElse(null);
    }

    @Nullable
    private String getAuthorityFromDto(DashboardDto dto) {
        return StringUtils.isBlank(dto.getAuthority()) ? null : dto.getAuthority();
    }

    @Nullable
    private EntityRef getAppliedToRefFromDto(DashboardDto dto) {
        EntityRef recordRef = dto.getAppliedToRef();
        if (EntityRef.isNotEmpty(recordRef) && recordRef.getAppName().isEmpty()) {
            recordRef = recordRef.withAppName(AppName.ALFRESCO);
        }
        return recordRef;
    }

    private DashboardEntity mapToEntity(DashboardDto dto) {
        return mapToEntity(dto, findEntityForDto(dto));
    }

    private DashboardEntity mapToEntity(DashboardDto dto, @Nullable DashboardEntity entity) {

        EntityRef appliedToRef = getAppliedToRefFromDto(dto);

        if (entity == null) {

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
            newDashboard.setAuthority(getAuthorityFromDto(dto));
            newDashboard.setTypeRef(EntityRef.toString(dto.getTypeRef()));
            if (EntityRef.isNotEmpty(appliedToRef)) {
                newDashboard.setAppliedToRef(EntityRef.toString(appliedToRef));
            }
            newDashboard.setScope(StringUtils.defaultString(dto.getScope()));
            entity = newDashboard;
        }

        if (dto.getConfig() != null && !dto.getConfig().isEmpty()) {
            entity.setConfig(Json.getMapper().toBytes(dto.getConfig()));
        }
        if (!MLText.isEmpty(dto.getName())) {
            entity.setName(Json.getMapper().toString(dto.getName()));
        }
        if (StringUtils.isNotBlank(dto.getScope())) {
            entity.setScope(dto.getScope());
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
