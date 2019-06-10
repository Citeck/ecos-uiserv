package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.CrudRecordsDAO;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;
import ru.citeck.ecos.uiserv.service.RecordNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Component
@RequiredArgsConstructor
public class DashboardRecords extends CrudRecordsDAO<DashboardDTO> {

    public static final String ID = "dashboard";

    private final DashboardService dashboardService;

    {
        setId(ID);
    }

    @Override
    public List<DashboardDTO> getValuesToMutate(List<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(id ->
                Optional.of(id)
                    .filter(str -> !str.isEmpty())
                    .map(x -> dashboardService.getById(x)
                        .orElseThrow(() -> new RecordNotFoundException("Dashboard with id " + id + " not found!")))
                    .orElseGet(DashboardDTO::new))
            .collect(Collectors.toList());
    }

    @Override
    public RecordsMutResult save(List<DashboardDTO> values) {
        RecordsMutResult recordsMutResult = new RecordsMutResult();
        values.forEach(dashboardDTO -> {
            DashboardDTO saved = dashboardService.save(dashboardDTO);
            RecordMeta recordMeta = new RecordMeta(saved.getId());
            recordsMutResult.addRecord(recordMeta);
        });
        return recordsMutResult;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        List<RecordMeta> resultRecords = new ArrayList<>();
        deletion.getRecords()
            .forEach(recordRef -> {
                dashboardService.delete(recordRef.getId());
                resultRecords.add(new RecordMeta(recordRef));
            });

        RecordsDelResult result = new RecordsDelResult();
        result.setRecords(resultRecords);
        return result;
    }

    @Override
    public List<DashboardDTO> getMetaValues(List<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(id -> Optional.of(id)
                .filter(str -> !str.isEmpty())
                .map(x -> dashboardService.getById(x)
                    .orElseThrow(() -> new RecordNotFoundException("Dashboard with id " + id + " not found!")))
                .orElseGet(() -> {
                    final DashboardDTO form = new DashboardDTO();
                    form.setId("");
                    return form;
                }))
            .collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<DashboardDTO> getMetaValues(RecordsQuery recordsQuery) {
        String language = recordsQuery.getLanguage();
        if (StringUtils.isNotBlank(language)) {
            throw new IllegalArgumentException("This records source does not support query via language");
        }

        RecordsQueryResult<DashboardDTO> result = new RecordsQueryResult<>();
        DashboardQuery dashboardQuery = recordsQuery.getQuery(DashboardQuery.class);
        Optional<DashboardDTO> dashboard = Optional.empty();

        if (StringUtils.isNotBlank(dashboardQuery.key)) {
            dashboard = dashboardService.getByKeys(Arrays.stream(dashboardQuery.key.split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList()));
        } else if (dashboardQuery.record != null) {
            dashboard = dashboardService.getByRecord(dashboardQuery.record);
        }

        dashboard
            .map(Collections::singletonList)
            .ifPresent(list -> {
                result.setRecords(list);
                result.setTotalCount(list.size());
            });

        return result;
    }

    private static class DashboardQuery {
        @Getter
        @Setter
        private String key;

        @Getter
        @Setter
        private RecordRef record;
    }

}
