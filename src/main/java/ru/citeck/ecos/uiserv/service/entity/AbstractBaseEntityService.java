package ru.citeck.ecos.uiserv.service.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
@Log4j2
public abstract class AbstractBaseEntityService<T> implements BaseEntityService<T> {

    private final Class<T> typeParameterClass;

    protected FileType type;
    protected ObjectMapper objectMapper;
    protected FileService fileService;

    public AbstractBaseEntityService(Class<T> typeParameterClass, FileType fileType) {
        this.typeParameterClass = typeParameterClass;
        this.type = fileType;
    }

    @Override
    public abstract T create(T entity);

    @Override
    public abstract T update(T entity);

    @Override
    public void delete(String id) {
        fileService.deployFileOverride(type, id, null, null, null);
    }

    @Override
    public Optional<T> getById(String id) {
        return fileService.loadFile(type, id)
            .map(this::fromJson);
    }

    private T fromJson(File file) {
        try {
            return objectMapper.readValue(file.getFileVersion().getBytes(), typeParameterClass);
        } catch (IOException e) {
            throw new RuntimeException("Failed convert File to DashboardDTO", e);
        }
    }

    @Override
    public Optional<T> getByKey(String key) {
        List<File> found = fileService.find("key", Collections.singletonList(key));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        if (found.size() > 1) {
            log.warn(String.format("More than one entity <%s> found by key: %s", typeParameterClass, key));
        }
        return Optional.of(fromJson(found.get(0)));
    }

    @Override
    public Optional<T> getByKeys(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Optional.empty();
        }

        return keys.stream()
            .map(this::getByKey)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    @Override
    public abstract Optional<T> getByRecord(RecordRef recordRef);

    protected byte[] toJson(T entity) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectMapper.writeValue(output, entity);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed convert entity <%s> to json", typeParameterClass), e);
        }
    }

}
