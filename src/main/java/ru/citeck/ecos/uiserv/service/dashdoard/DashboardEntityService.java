package ru.citeck.ecos.uiserv.service.dashdoard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.entity.AbstractBaseEntityService;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.util.*;

/**
 * @author Roman Makarskiy
 */
@Service
public class DashboardEntityService extends AbstractBaseEntityService<DashboardDTO> {

    private static final String TYPE_REFIX = "type_";

    private static final String META_KEY = "key";
    private static final String META_TYPE = "type";
    private static final String META_USER = "user";

    public DashboardEntityService(ObjectMapper objectMapper, FileService fileService) {
        super(DashboardDTO.class, FileType.DASHBOARD);
        this.objectMapper = objectMapper;
        this.fileService = fileService;
    }

    @Override
    public DashboardDTO create(DashboardDTO entity) {
        if (StringUtils.isBlank(entity.getKey())) {
            throw new IllegalArgumentException("Key is mandatory for creating dashboard");
        }

        Optional<DashboardDTO> optional = getByKey(entity.getType(), entity.getKey(), entity.getUser());
        if (optional.isPresent()) {
            entity.setId(optional.get().getId());
            return update(entity);
        } else {
            String id = StringUtils.isNotBlank(entity.getId()) ? entity.getId() : UUID.randomUUID().toString();
            return saveWithId(id, entity);
        }
    }

    @Override
    public DashboardDTO update(DashboardDTO entity) {
        if (entity.getId() == null) {
            return create(entity);
        }
        return saveWithId(entity.getId(), entity);
    }

    @Override
    public Optional<DashboardDTO> getByKey(String type, String key, String user) {
        if (type == null) {
            type = "case-details";
        }
        Optional<DashboardDTO> result = super.getByKey(type, key, user);
        if (!result.isPresent() && key.startsWith(TYPE_REFIX)) {
            result = super.getByKey(type, key.substring(TYPE_REFIX.length()), user);
        }
        return result;
    }

    private DashboardDTO saveWithId(String id, DashboardDTO entity) {
        DashboardDTO result = new DashboardDTO();

        result.setId(id);
        result.setKey(entity.getKey());
        result.setType(entity.getType());
        result.setConfig(entity.getConfig());
        result.setUser(entity.getUser());

        writeToFile(result);
        return result;
    }

    private void writeToFile(DashboardDTO entity) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(META_KEY, entity.getKey());
        metadata.put(META_TYPE, entity.getType());
        metadata.put(META_USER, entity.getUser());
        fileService.deployFileOverride(type, entity.getId(), null, toJson(entity), metadata);
    }

    @Override
    public Optional<DashboardDTO> getByRecord(RecordRef recordRef) {
        return Optional.empty();
    }
}
