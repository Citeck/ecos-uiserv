package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.uiserv.domain.DashboardDto;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DashboardRecords extends LocalRecordsDAO
                              implements LocalRecordsQueryWithMetaDAO<DashboardDto>,
                                         LocalRecordsMetaDAO<DashboardDto> {

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
    public RecordsQueryResult<DashboardDto> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {

        String language = recordsQuery.getLanguage();
        if (StringUtils.isNotBlank(language)) {
            throw new IllegalArgumentException("This records source does not support query via language");
        }

        Query query = recordsQuery.getQuery(Query.class);

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

        Optional<DashboardDto> dashboard = dashboardService.getForAuthority(
            query.getTypeRef(),
            query.getAuthority(),
            query.expandType,
            query.includeForAll
        );

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

    @Data
    private static class Query {
        private RecordRef recordRef;
        private RecordRef typeRef;
        private String authority;
        private boolean expandType = true;
        private boolean includeForAll = true;
    }
}
