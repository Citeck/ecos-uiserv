package ru.citeck.ecos.uiserv.service.dashdoard;

import ru.citeck.ecos.uiserv.domain.DashboardDTO;

/**
 * @author Roman Makarskiy
 */
public interface DashboardRepository {

    DashboardDTO create(DashboardDTO dashboardDTO);

    DashboardDTO read(String id);

    DashboardDTO update(DashboardDTO dashboardDTO);

    void delete(String id);

    DashboardDTO getByKey(String key);

}
