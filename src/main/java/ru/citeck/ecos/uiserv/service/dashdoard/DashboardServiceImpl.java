package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;

import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
@Log4j2
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;

    @Setter
    private RecordsService recordsService;

    @Override
    public Optional<DashboardDTO> getById(String id) {
        return Optional.ofNullable(dashboardRepository.read(id));
    }

    @Override
    public Optional<DashboardDTO> getByKey(String key) {
        return Optional.ofNullable(dashboardRepository.getByKey(key));
    }

    @Override
    public Optional<DashboardDTO> getByKeys(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Optional.empty();
        }

        return keys.stream()
            .map(this::getByKey)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    //TODO: test
    @Override
    public Optional<DashboardDTO> getByRecord(RecordRef record) {
        DashboardServiceImpl.DashboardKey keys = recordsService.getMeta(record, DashboardServiceImpl.DashboardKey.class);
        return getByKeys(keys.getKeys());
    }

    @Override
    public DashboardDTO save(DashboardDTO dashboard) {
        DashboardDTO result;

        if (StringUtils.isBlank(dashboard.getId())) {
            result = dashboardRepository.create(dashboard);
        } else {
            result = dashboardRepository.update(dashboard);
        }

        return result;
    }

    @Override
    public void delete(String id) {
        dashboardRepository.delete(id);
    }

    private static class DashboardKey {
        private final static String ATT_DASHBOARD_KEY = "_dashboardKey";

        @MetaAtt(ATT_DASHBOARD_KEY)
        @Getter
        @Setter
        private List<String> keys;
    }

}
