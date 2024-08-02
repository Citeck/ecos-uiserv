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
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;

import jakarta.annotation.PostConstruct;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.nio.charset.StandardCharsets;
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

    @NotNull
    @Override
    public String mutate(@NotNull LocalRecordAtts record) throws Exception {

        Optional<DashboardDto> dashboardDto = Optional.empty();
        if (!record.getId().isBlank()) {
            dashboardDto = dashboardService.getDashboardById(record.getId());
            if (dashboardDto.isEmpty()) {
                throw new IllegalArgumentException("Dashboard with id '" + record.getId() + "' is not found!");
            }
        }
        if (dashboardDto.isEmpty()) {
            String newId = record.getAtt("id", "");
            if (!newId.isBlank()) {
                dashboardDto = dashboardService.getDashboardById(newId);
            }
        }

        String idBefore = dashboardDto.map(DashboardDto::getId).orElse("");
        DashboardRecord toMutate = dashboardDto.map(DashboardRecord::new).orElseGet(DashboardRecord::new);

        Json.getMapper().applyData(toMutate, record.getAtts());

        if (!idBefore.isBlank()
                && !idBefore.equals(toMutate.getId())
                && dashboardService.getDashboardById(toMutate.getId()).isPresent()
        ) {
            throw new RuntimeException("Record with id '" + toMutate.getId() + "' already exists");
        }

        return dashboardService.saveDashboard(toMutate).getId();
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

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {

            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            List<DashboardRecord> records = dashboardService.findAll(
                predicate,
                recordsQuery.getPage().getMaxItems(),
                recordsQuery.getPage().getSkipCount(),
                recordsQuery.getSortBy()
            ).stream()
                .map(DashboardRecord::new)
                .collect(Collectors.toList());

            RecsQueryRes<DashboardRecord> result = new RecsQueryRes<>();
            result.setRecords(records);
            result.setTotalCount(dashboardService.getCount(predicate));

            return result;
        }

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
        public DashboardDto toJson() {
            return new DashboardDto(this);
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
        private boolean expandType = true;
        private boolean includeForAll = true;
    }
}
