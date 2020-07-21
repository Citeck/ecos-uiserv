package ru.citeck.ecos.uiserv.domain.dashdoard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.OldDashboardDto;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.entity.AbstractBaseEntityService;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.util.*;

/**
 * @author Roman Makarskiy
 */
@Service
@Deprecated
public class DashboardEntityService extends AbstractBaseEntityService<OldDashboardDto> {

    private static final String TYPE_PREFIX = "type_";

    private static final String META_KEY = "key";
    private static final String META_TYPE = "type";
    private static final String META_USER = "user";

    public DashboardEntityService(ObjectMapper objectMapper, FileService fileService) {
        super(OldDashboardDto.class, FileType.DASHBOARD);
        this.objectMapper = objectMapper;
        this.fileService = fileService;
    }

    @Override
    public OldDashboardDto create(OldDashboardDto entity) {
        if (StringUtils.isBlank(entity.getKey())) {
            throw new IllegalArgumentException("Key is mandatory for creating dashboard");
        }
        Optional<OldDashboardDto> optional = getByKey(entity.getType(), entity.getKey(), entity.getUser());
        if (optional.isPresent()) {
            entity.setId(optional.get().getId());
            return update(entity);
        } else {
            String id = StringUtils.isNotBlank(entity.getId()) ? entity.getId() : UUID.randomUUID().toString();
            return saveWithId(id, entity);
        }
    }

    @Override
    public OldDashboardDto update(OldDashboardDto entity) {
        if (entity.getId() == null) {
            return create(entity);
        }
        return saveWithId(entity.getId(), entity);
    }

    @Override
    public Optional<OldDashboardDto> getByKey(String type, String key, String user) {
        if (type == null) {
            type = "case-details";
        }
        Optional<OldDashboardDto> result = super.getByKey(type, key, user);
        if (!result.isPresent() && key.startsWith(TYPE_PREFIX)) {
            result = super.getByKey(type, key.substring(TYPE_PREFIX.length()), user);
        }
        return result;
    }

    private OldDashboardDto saveWithId(String id, OldDashboardDto entity) {
        OldDashboardDto result = new OldDashboardDto();

        result.setId(id);
        result.setKey(entity.getKey());
        result.setType(entity.getType());
        result.setConfig(entity.getConfig());
        result.setUser(entity.getUser());

        writeToFile(result);
        return result;
    }

    private void writeToFile(OldDashboardDto entity) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(META_KEY, entity.getKey());
        metadata.put(META_TYPE, entity.getType());
        metadata.put(META_USER, entity.getUser());
        fileService.deployFileOverride(type, entity.getId(), null, toJson(entity), metadata);
    }

    @Override
    public Optional<OldDashboardDto> getByRecord(RecordRef recordRef) {
        return Optional.empty();
    }
}
