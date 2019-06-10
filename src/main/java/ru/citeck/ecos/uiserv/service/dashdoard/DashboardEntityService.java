package ru.citeck.ecos.uiserv.service.dashdoard;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.entity.AbstractBaseEntityService;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Roman Makarskiy
 */
@Service("DashboardEntityService")
public class DashboardEntityService extends AbstractBaseEntityService<DashboardDTO> {

    private static final String KEY = "key";

    private RecordsService recordsService;

    public DashboardEntityService(@Lazy RecordsService recordsService,
                                  ObjectMapper objectMapper, FileService fileService) {
        super(DashboardDTO.class, FileType.DASHBOARD);
        this.recordsService = recordsService;
        this.objectMapper = objectMapper;
        this.fileService = fileService;
    }

    @Override
    public DashboardDTO create(DashboardDTO entity) {
        String id = StringUtils.isNotBlank(entity.getId()) ? entity.getId() : UUID.randomUUID().toString();
        return saveWithId(id, entity);
    }

    @Override
    public DashboardDTO update(DashboardDTO entity) {
        return saveWithId(entity.getId(), entity);
    }

    private DashboardDTO saveWithId(String id, DashboardDTO entity) {
        DashboardDTO result = new DashboardDTO();

        result.setId(id);
        result.setKey(entity.getKey());
        result.setConfig(entity.getConfig());

        writeToFile(result);
        return result;
    }

    private void writeToFile(DashboardDTO entity) {
        fileService.deployFileOverride(type, entity.getId(), null,
            toJson(entity), Collections.singletonMap(KEY, entity.getKey()));
    }

    @Override
    public Optional<DashboardDTO> getByRecord(RecordRef recordRef) {
        DashboardKey keys = recordsService.getMeta(recordRef, DashboardKey.class);
        return getByKeys(keys.getKeys());
    }

    private static class DashboardKey {
        private final static String ATT_DASHBOARD_KEY = "_dashboardKey";

        @MetaAtt(ATT_DASHBOARD_KEY)
        @Getter
        @Setter
        private List<String> keys;
    }
}
