package ru.citeck.ecos.uiserv.domain.dashdoard.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
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
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardRecords extends AbstractRecordsDao
    implements RecordsQueryDao,
               RecordMutateDtoDao<DashboardRecords.DashboardRecord>,
               RecordDeleteDao,
               RecordAttsDao {

    private static final RecordRef DEFAULT_TYPE = RecordRef.valueOf("emodel/type@user-dashboard");

    public static final String ID = "dashboard";

    private final DashboardService dashboardService;
    private final RecordEventsService recordEventsService;

    @PostConstruct
    public void init() {
        dashboardService.addChangeListener((before, after) ->
            recordEventsService.emitRecChanged(before, after, getId(), DashboardRecord::new));
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String dashboardId) {
        dashboardService.removeDashboard(dashboardId);
        return DelStatus.OK;
    }

    @Override
    public DashboardRecord getRecToMutate(@NotNull String dashboardId) {
        if (dashboardId.isEmpty()) {
            return new DashboardRecord();
        }
        return dashboardService.getDashboardById(dashboardId)
            .map(DashboardRecord::new)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard with id '" + dashboardId + "' is not found!"));
    }

    @NotNull
    @Override
    public String saveMutatedRec(DashboardRecord dashboardRecord) {
        return dashboardService.saveDashboard(dashboardRecord).getId();
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String dashboardId) {
        if (dashboardId.isEmpty()) {
            return new DashboardRecord();
        }
        return dashboardService.getDashboardById(dashboardId)
            .map(DashboardRecord::new)
            .orElse(null);
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
                DataValue ecosType = recordsService.getAtt(query.getRecordRef(), "_type?id");
                if (ecosType.isTextual()) {
                    query.setTypeRef(RecordRef.valueOf(ecosType.asText()));
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

        public String getEcosType() {
            return "dashboard";
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String dataUriContent = content.get(0).get("url", "");
            ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);

            Json.getMapper().applyData(this, data);
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        public DashboardDto toJson() {
            return new DashboardDto(this);
        }

        public byte[] getData() {
            return YamlUtils.toNonDefaultString(toJson()).getBytes(StandardCharsets.UTF_8);
        }
    }

    @Data
    public static class Query {
        private RecordRef recordRef;
        private RecordRef typeRef;
        private String authority;
        private boolean expandType = true;
        private boolean includeForAll = true;
    }
}
