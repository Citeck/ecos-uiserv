package ru.citeck.ecos.uiserv.domain.dashdoard.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.record.op.delete.dao.RecordDeleteDao;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DashboardRecords extends AbstractRecordsDao
    implements RecordsQueryDao,
               RecordMutateDtoDao<DashboardRecords.DashboardRecord>,
               RecordDeleteDao,
               RecordAttsDao {

    private static final RecordRef DEFAULT_TYPE = RecordRef.valueOf("emodel/type@user-dashboard");

    public static final String ID = "dashboard";

    private final DashboardService dashboardService;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Autowired
    public DashboardRecords(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String dashboardId) {
        dashboardService.removeDashboard(dashboardId);
        return DelStatus.OK;
    }

    @Override
    public DashboardRecord getRecToMutate(@NotNull String dashboardId) {
        return new DashboardRecord(getDashboardRecord(dashboardId));
    }

    @NotNull
    @Override
    public String saveMutatedRec(DashboardRecord dashboardRecord) {
        return dashboardService.saveDashboard(dashboardRecord).getId();
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String dashboardId) {
        return getDashboardRecord(dashboardId);
    }

    @NotNull
    private DashboardRecord getDashboardRecord(@NotNull String dashboardId) {

        if (dashboardId.isEmpty()) {
            return new DashboardRecord();
        }
        return dashboardService.getDashboardById(dashboardId)
            .map(DashboardRecord::new)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard with id '" + dashboardId + "' is not found!"));
    }

    @Nullable
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {

        Query query = recordsQuery.getQueryOrNull(Query.class);

        if (query == null
                || "criteria".equals(recordsQuery.getLanguage())
                || "predicate".equals(recordsQuery.getLanguage())) {

            RecsQueryRes<DashboardRecord> result = new RecsQueryRes<>();
            result.setRecords(dashboardService.getAllDashboards()
                .stream()
                .map(DashboardRecord::new)
                .collect(Collectors.toList()));

            return result;
        }

        if (query.getTypeRef() == null) {
            if (query.getRecordRef() != null) {
                DataValue ecosType = recordsService.getAtt(query.getRecordRef(), "_etype?id");
                if (ecosType.isTextual()) {
                    query.setTypeRef(RecordRef.valueOf(ecosType.asText()));
                }
            }
            if (query.getTypeRef() == null) {
                query.setTypeRef(DEFAULT_TYPE);
            }
        }

        Optional<DashboardRecord> dashboard = dashboardService.getForAuthority(
            query.getTypeRef(),
            query.getAuthority(),
            query.expandType,
            query.includeForAll
        ).map(DashboardRecord::new);

        return dashboard.map(RecsQueryRes::of).orElseGet(RecsQueryRes::new);
    }

    public static class DashboardRecord extends DashboardDto {

        DashboardRecord() {
        }

        DashboardRecord(DashboardDto dto) {
            super(dto);
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
            return result != null ? result : "Dashboard";
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String base64Content = content.get(0).get("url", "");
            base64Content = base64Content.replaceAll("^data:application/json;base64,", "");
            ObjectData data = Json.getMapper().read(Base64.getDecoder().decode(base64Content), ObjectData.class);

            Json.getMapper().applyData(this, data);
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        public DashboardDto toJson() {
            return new DashboardDto(this);
        }
    }

    @Data
    private static class Query {
        private RecordRef recordRef;
        private RecordRef typeRef;
        private String authority;
        private boolean expandType = true;
        private boolean includeForAll = true;
    }
}
