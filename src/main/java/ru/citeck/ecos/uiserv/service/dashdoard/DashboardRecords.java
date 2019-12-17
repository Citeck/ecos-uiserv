package ru.citeck.ecos.uiserv.service.dashdoard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.uiserv.domain.DashboardDto;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        List<RecordMeta> records = values.stream()
            .map(this::save)
            .collect(Collectors.toList());

        RecordsMutResult recordsMutResult = new RecordsMutResult();
        recordsMutResult.setRecords(records);
        return recordsMutResult;
    }

    private RecordMeta save(DashboardDto dto) {

        DashboardDto saved;

        Optional<DashboardDto> optionalDashboardDto = entityService.getByKey(dto.getType(), dto.getKey(), dto.getUser());
        if (optionalDashboardDto.isPresent()) {
            DashboardDto storedDashboardDto = optionalDashboardDto.get();
            storedDashboardDto.setConfig(dto.getConfig());
            saved = entityService.update(storedDashboardDto);
        } else {
            saved = entityService.create(dto);
        }

        return new RecordMeta(saved.getId());
    }

    @Override
    protected DashboardDto getEmpty() {
        return new DashboardDto();
    }
}
