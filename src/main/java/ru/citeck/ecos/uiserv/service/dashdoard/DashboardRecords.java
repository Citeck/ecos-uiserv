package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.uiserv.domain.DashboardDto;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DashboardRecords extends LocalRecordsDAO
                              implements LocalRecordsQueryWithMetaDAO<DashboardDto>,
                                         LocalRecordsMetaDAO<DashboardDto>,
                                         MutableRecordsLocalDAO<DashboardDto> {

    private static final String DEFAULT_KEY = "DEFAULT";
    public static final String ID = "dashboard";

    private static final long PUBLISH_TIMEOUT_MS = 10_000;

    private final DashboardService dashboardService;

    @Autowired
    public DashboardRecords(DashboardService dashboardService,
                            RecordsService recordsService) {
        setId(ID);
        this.dashboardService = dashboardService;
        this.recordsService = recordsService;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion recordsDeletion) {
        throw new UnsupportedOperationException("Delete is not supported");
    }

    @Override
    public List<DashboardDto> getValuesToMutate(List<RecordRef> list) {
        return list.stream()
            .map(this::getDashboardByRef)
            .collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<DashboardDto> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {

        String language = recordsQuery.getLanguage();
        if (StringUtils.isNotBlank(language)) {
            throw new IllegalArgumentException("This records source does not support query via language");
        }

        Query query = recordsQuery.getQuery(Query.class);

        if (StringUtils.isBlank(query.type)) {
            return new RecordsQueryResult<>();
        }

        if (StringUtils.isBlank(query.key)) {
            query.key = DEFAULT_KEY;
        }

        List<String> keys = Arrays.asList(query.key.split(","));
        Optional<DashboardDto> dashboard = dashboardService.getFirstDashboardByKeys(query.type, keys, query.user);

        return dashboard.map(RecordsQueryResult::of).orElseGet(RecordsQueryResult::new);
    }

    @Override
    public List<DashboardDto> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {
        return list.stream()
            .map(this::getDashboardByRef)
            .collect(Collectors.toList());
    }

    private DashboardDto getDashboardByRef(RecordRef ref) {

        if (ref.getId().isEmpty()) {
            return new DashboardDto();
        }

        return dashboardService.getDashboardById(ref.getId())
            .orElseThrow(() -> new IllegalArgumentException("Dashboard with id '" + ref + "' is not found!"));
    }

    @Override
    public RecordsMutResult save(List<DashboardDto> values) {

        List<RecordMeta> resultMeta = values.stream()
            .map(dashboardService::saveDashboardWithEapps)
            .map(f -> getDashboardFromFuture(f, values))
            .map(f -> new RecordMeta(RecordRef.valueOf(f.getId())))
            .collect(Collectors.toList());

        RecordsMutResult result = new RecordsMutResult();
        result.setRecords(resultMeta);
        return result;
    }

    private DashboardDto getDashboardFromFuture(Future<DashboardDto> future, List<DashboardDto> values) {
        try {
            return future.get(PUBLISH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Publish waiting failed. Values: " + values, e);
        }
    }

    @Data
    private static class Query {
        private String key;
        private String type;
        private String user;
    }
}
