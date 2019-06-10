package ru.citeck.ecos.uiserv.service.dashdoard;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;

import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
public interface DashboardService {

    Optional<DashboardDTO> getById(String id);

    Optional<DashboardDTO> getByKey(String key);

    Optional<DashboardDTO> getByKeys(List<String> keys);

    Optional<DashboardDTO> getByRecord(RecordRef record);

    DashboardDTO save(DashboardDTO dashboard);

    void delete(String id);

}
