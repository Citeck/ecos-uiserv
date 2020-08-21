package ru.citeck.ecos.uiserv.domain.dashdoard.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DashboardRecords extends LocalRecordsDao
                              implements LocalRecordsQueryWithMetaDao<DashboardRecords.DashboardRecord>,
                                         LocalRecordsMetaDao<DashboardRecords.DashboardRecord>,
                                         MutableRecordsLocalDao<DashboardRecords.DashboardRecord> {

    private static final RecordRef DEFAULT_TYPE = RecordRef.valueOf("emodel/type@user-dashboard");

    public static final String ID = "dashboard";

    private final DashboardService dashboardService;

    @Autowired
    public DashboardRecords(DashboardService dashboardService,
                            RecordsService recordsService) {
        setId(ID);
        this.dashboardService = dashboardService;
        this.recordsService = recordsService;
    }

    @Override
    public RecordsQueryResult<DashboardRecord> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                                 @NotNull MetaField metaField) {

        Query query = recordsQuery.getQuery(Query.class);

        if (query == null
                || "criteria".equals(recordsQuery.getLanguage())
                || "predicate".equals(recordsQuery.getLanguage())) {

            RecordsQueryResult<DashboardRecord> result = new RecordsQueryResult<>();
            result.setRecords(dashboardService.getAllDashboards()
                .stream()
                .map(DashboardRecord::new)
                .collect(Collectors.toList()));
            return result;
        }

        if (query.getTypeRef() == null) {
            if (query.getRecordRef() != null) {
                DataValue ecosType = recordsService.getAttribute(query.getRecordRef(), "_etype?id");
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

        return dashboard.map(RecordsQueryResult::of).orElseGet(RecordsQueryResult::new);
    }

    @Override
    public List<DashboardRecord> getLocalRecordsMeta(@NotNull List<RecordRef> list,
                                                     @NotNull MetaField metaField) {
        return list.stream()
            .map(this::getDashboardByRef)
            .collect(Collectors.toList());
    }

    private DashboardRecord getDashboardByRef(RecordRef ref) {

        if (ref.getId().isEmpty()) {
            return new DashboardRecord();
        }

        return dashboardService.getDashboardById(ref.getId())
            .map(DashboardRecord::new)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard with id '" + ref + "' is not found!"));
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion recordsDeletion) {
        List<RecordMeta> resultMeta = new ArrayList<>();
        recordsDeletion.getRecords().forEach(r -> {
            dashboardService.removeDashboard(r.getId());
            resultMeta.add(new RecordMeta(r));
        });
        RecordsDelResult result = new RecordsDelResult();
        result.setRecords(resultMeta);
        return result;
    }

    @NotNull
    @Override
    public List<DashboardRecord> getValuesToMutate(@NotNull List<RecordRef> list) {
        return list.stream()
            .map(ref -> new DashboardRecord(getDashboardByRef(ref)))
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public RecordsMutResult save(@NotNull List<DashboardRecord> values) {
        RecordsMutResult result = new RecordsMutResult();
        values.forEach(value -> {
            DashboardDto dashboardDto = dashboardService.saveDashboard(value);
            result.addRecord(new RecordMeta(RecordRef.valueOf(dashboardDto.getId())));
        });
        return result;
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
        @MetaAtt(".disp")
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