package ru.citeck.ecos.uiserv.domain.dashdoard.api.records;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.events2.type.RecordEventsService;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records3.record.atts.schema.ScalarType;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.commons.data.entity.EntityWithMeta;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;

import jakarta.annotation.PostConstruct;
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardRecords extends AbstractRecordsDao
    implements RecordsQueryDao,
               RecordMutateDao,
               RecordDeleteDao,
               RecordAttsDao {

    private static final EntityRef DEFAULT_TYPE = EntityRef.valueOf("emodel/type@user-dashboard");

    public static final String ID = "dashboard";

    private final DashboardService dashboardService;
    private final RecordEventsService recordEventsService;
    private final EcosAuthoritiesApi authoritiesApi;
    private final WorkspaceService workspaceService;

    @PostConstruct
    public void init() {
        dashboardService.addChangeListener((before, after) ->
            recordEventsService.emitRecChanged(before, after, getId(),
                dto -> new DashboardRecord(dto, workspaceService)));
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String dashboardId) {
        IdInWs idInWs = workspaceService.convertToIdInWs(dashboardId);
        dashboardService.removeDashboard(idInWs.getId(), idInWs.getWorkspace());
        return DelStatus.OK;
    }

    @NotNull
    @Override
    public String mutate(@NotNull LocalRecordAtts record) throws Exception {

        Optional<DashboardDto> dashboardDto = Optional.empty();
        if (!record.getId().isBlank()) {
            IdInWs idInWs = workspaceService.convertToIdInWs(record.getId());
            dashboardDto = dashboardService.getDashboardById(idInWs.getId(), idInWs.getWorkspace());
            if (dashboardDto.isEmpty()) {
                throw new IllegalArgumentException("Dashboard with id '" + record.getId() + "' is not found!");
            }
        }
        if (dashboardDto.isEmpty()) {
            String newId = record.getAtt("id", "");
            if (!newId.isBlank()) {
                // newId comes from the mutation payload (not the record ref), so it carries no
                // workspace prefix to parse — take the workspace from the payload's explicit field.
                String wsForLookup = record.getAtt("workspace", "");
                dashboardDto = dashboardService.getDashboardById(newId, wsForLookup);
            }
        }

        String idBefore = dashboardDto.map(DashboardDto::getId).orElse("");
        DashboardRecord toMutate = dashboardDto.map(DashboardRecord::new).orElseGet(DashboardRecord::new);

        Json.getMapper().applyData(toMutate, record.getAtts());

        if (!idBefore.isBlank()
                && !idBefore.equals(toMutate.getId())
                && dashboardService.getDashboardById(toMutate.getId(), toMutate.getWorkspace()).isPresent()
        ) {
            throw new RuntimeException("Record with id '" + toMutate.getId() + "' already exists");
        }

        DashboardDto saved = dashboardService.saveDashboard(toMutate.build());
        return workspaceService.addWsPrefixToId(saved.getId(), saved.getWorkspace());
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String dashboardId) {
        if (dashboardId.isEmpty()) {
            return new DashboardRecord();
        }
        IdInWs idInWs = workspaceService.convertToIdInWs(dashboardId);
        return dashboardService.getDashboardWithMeta(idInWs.getId(), idInWs.getWorkspace())
            .map(wm -> new DashboardRecord(wm, authoritiesApi, workspaceService))
            .orElse(null);
    }

    @Nullable
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {

            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            List<DashboardRecord> records = dashboardService.findAllWithMeta(
                predicate,
                recordsQuery.getWorkspaces(),
                recordsQuery.getPage().getMaxItems(),
                recordsQuery.getPage().getSkipCount(),
                recordsQuery.getSortBy()
            ).stream()
                .map(wm -> new DashboardRecord(wm, authoritiesApi, workspaceService))
                .collect(Collectors.toList());

            RecsQueryRes<DashboardRecord> result = new RecsQueryRes<>();
            result.setRecords(records);
            result.setTotalCount(dashboardService.getCount(predicate, recordsQuery.getWorkspaces()));

            return result;
        }

        Query query = recordsQuery.getQueryOrNull(Query.class);

        if (query == null
                || "criteria".equals(recordsQuery.getLanguage())
                || "predicate".equals(recordsQuery.getLanguage())) {

            RecsQueryRes<DashboardRecord> result = new RecsQueryRes<>();
            result.setRecords(dashboardService.getAllDashboards()
                .stream()
                .map(dto -> new DashboardRecord(dto, workspaceService))
                .collect(Collectors.toList()));

            return result;
        }

        if (query.getTypeRef() == null) {
            if (query.getRecordRef() != null) {
                DataValue ecosType = recordsService.getAtt(query.getRecordRef(), "_type?id");
                if (ecosType.isTextual()) {
                    query.setTypeRef(EntityRef.valueOf(ecosType.asText()));
                }
            }
            if (query.getTypeRef() == null) {
                query.setTypeRef(DEFAULT_TYPE);
            }
        }

        Optional<DashboardRecord> dashboard = dashboardService.getForAuthority(
            query.getRecordRef(),
            query.getTypeRef(),
            query.getAuthority(),
            query.scope,
            query.workspace,
            query.expandType,
            query.includeForAll
        ).map(dto -> new DashboardRecord(dto, workspaceService));

        return dashboard.map(RecsQueryRes::of).orElseGet(RecsQueryRes::new);
    }

    public static class DashboardRecord extends DashboardDto.Builder {

        @Nullable
        private final Instant createdDate;
        @Nullable
        private final Instant lastModifiedDate;
        @Nullable
        private final String createdBy;
        @Nullable
        private final String lastModifiedBy;
        @Nullable
        private final EcosAuthoritiesApi authoritiesApi;
        @Nullable
        private final WorkspaceService workspaceService;

        DashboardRecord() {
            this.createdDate = null;
            this.lastModifiedDate = null;
            this.createdBy = null;
            this.lastModifiedBy = null;
            this.authoritiesApi = null;
            this.workspaceService = null;
        }

        // Mutation-only: workspaceService is null, so getRef() yields an un-prefixed id.
        // Do not use for read/query results — use the (dto, workspaceService) overload there.
        DashboardRecord(DashboardDto dto) {
            this(dto, null);
        }

        DashboardRecord(DashboardDto dto, @Nullable WorkspaceService workspaceService) {
            super(dto);
            this.createdDate = null;
            this.lastModifiedDate = null;
            this.createdBy = null;
            this.lastModifiedBy = null;
            this.authoritiesApi = null;
            this.workspaceService = workspaceService;
        }

        DashboardRecord(EntityWithMeta<DashboardDto> withMeta,
                        @Nullable EcosAuthoritiesApi authoritiesApi,
                        @Nullable WorkspaceService workspaceService) {
            super(withMeta.getEntity());
            this.createdDate = withMeta.getMeta().getCreated();
            this.lastModifiedDate = withMeta.getMeta().getModified();
            this.createdBy = withMeta.getMeta().getCreator();
            this.lastModifiedBy = withMeta.getMeta().getModifier();
            this.authoritiesApi = authoritiesApi;
            this.workspaceService = workspaceService;
        }

        // Record id carries the workspace prefix (<wsSystemId>:localId) for workspace-scoped
        // dashboards, so that the same extId existing in multiple workspaces stays addressable
        // and resolves to a single row. Mirrors EcosFormRecord. Global dashboards keep a plain id.
        @AttName(ScalarType.ID_SCHEMA)
        public EntityRef getRef() {
            String localId = getId();
            if (workspaceService != null) {
                localId = workspaceService.addWsPrefixToId(localId, getWorkspace());
            }
            return EntityRef.create(AppName.UISERV, ID, localId);
        }

        public String getModuleId() {
            return getId();
        }

        public void setModuleId(String value) {
            setId(value);
        }

        @JsonIgnore
        @AttName("?disp")
        public String getDisplayName() {
            String result = getId();
            return !result.isEmpty() ? result : "Dashboard";
        }

        public String getEcosType() {
            return "dashboard";
        }

        @AttName(RecordConstants.ATT_CREATED)
        @Nullable
        public Instant getCreated() {
            return createdDate;
        }

        @AttName(RecordConstants.ATT_MODIFIED)
        @Nullable
        public Instant getModified() {
            return lastModifiedDate;
        }

        @AttName(RecordConstants.ATT_CREATOR)
        @Nullable
        public EntityRef getCreator() {
            if (authoritiesApi == null || createdBy == null || createdBy.isEmpty()) {
                return null;
            }
            return authoritiesApi.getPersonRef(createdBy);
        }

        @AttName(RecordConstants.ATT_MODIFIER)
        @Nullable
        public EntityRef getModifier() {
            if (authoritiesApi == null || lastModifiedBy == null || lastModifiedBy.isEmpty()) {
                return null;
            }
            return authoritiesApi.getPersonRef(lastModifiedBy);
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String dataUriContent = content.getFirst().get("url", "");
            ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);

            Json.getMapper().applyData(this, data);
        }

        @JsonValue
        public DashboardDto toJson() {
            return build();
        }

        public byte[] getData() {
            return YamlUtils.toNonDefaultString(toJson()).getBytes(StandardCharsets.UTF_8);
        }
    }

    @Data
    public static class Query {
        private EntityRef recordRef;
        private EntityRef typeRef;
        private String authority;
        private String scope;
        private String workspace = "";
        private boolean expandType = true;
        private boolean includeForAll = true;
    }
}
