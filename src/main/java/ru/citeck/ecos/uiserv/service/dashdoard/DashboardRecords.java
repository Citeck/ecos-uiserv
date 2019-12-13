package ru.citeck.ecos.uiserv.service.dashdoard;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.uiserv.domain.DashboardDto;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
@Component
public class DashboardRecords extends AbstractEntityRecords<DashboardDto> {

    public static final String ID = "dashboard";

    @Autowired
    public DashboardRecords(DashboardEntityService entityService) {
        setId(ID);
        this.entityService = entityService;
    }

    @Override
    public RecordsMutResult save(List<DashboardDto> values) {
        RecordsMutResult recordsMutResult = new RecordsMutResult();
        values.forEach(dto -> {

            if (StringUtils.isBlank(dto.getId())) {
                throw new IllegalArgumentException("Parameter 'id' is mandatory for config record");
            }

            Optional<DashboardDto> optionalDashboardDto = entityService.getByKey(
                dto.getType(),
                dto.getKey(),
                dto.getUser()
            );

            DashboardDto saved;
            if (optionalDashboardDto.isPresent()) {
                DashboardDto storedDashboardDto = optionalDashboardDto.get();
                storedDashboardDto.setConfig(dto.getConfig());
                saved = entityService.update(storedDashboardDto);
            } else {
                saved = entityService.create(dto);
            }

            RecordMeta recordMeta = new RecordMeta(saved.getId());
            recordsMutResult.addRecord(recordMeta);
        });
        return recordsMutResult;
    }

    @Override
    protected DashboardDto getEmpty() {
        return new DashboardDto();
    }
}
