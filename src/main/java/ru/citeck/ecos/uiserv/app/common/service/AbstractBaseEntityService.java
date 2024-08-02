package ru.citeck.ecos.uiserv.app.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import ru.citeck.ecos.uiserv.domain.file.repo.File;
import ru.citeck.ecos.uiserv.domain.file.repo.FileType;
import ru.citeck.ecos.uiserv.domain.file.service.FileService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Slf4j
public abstract class AbstractBaseEntityService<T extends EntityDto> implements BaseEntityService<T> {

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

    @Override
    public List<T> getAll() {
        return fileService.findByType(type)
            .stream()
            .map(f -> {
                try {
                    return Optional.ofNullable(fromJson(f));
                } catch (Exception e) {
                    return Optional.<T>empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    @Override
    public List<T> getAllByKey(String type, String key, String user) {

        List<File> found = fileService.find("key", Collections.singletonList(key));

        return found.stream()
            .map(this::fromJson)
            .filter(t -> {
                if (Objects.equals(t.getType(), type)) {
                    if (user == null) {
                        return t.getUser() == null;
                    }
                    return Objects.equals(t.getUser(), user);
                }
                return false;
            })
            .collect(Collectors.toList());
    }

    @Override
    public Optional<T> getByKey(String type, String key, String user) {

        List<T> entities = getAllByKey(type, key, user);

        if (entities.size() > 1) {
            log.warn(String.format("More than one entity <%s> found by type: '%s' and key: '%s'",
                typeParameterClass, type, key));
        }
        return entities.stream().findFirst();
    }

    @Override
    public Optional<T> getByKeys(String type, List<String> keys, String user) {
        if (CollectionUtils.isEmpty(keys)) {
            return Optional.empty();
        }

        return keys.stream()
            .map(key -> getByKey(type, key, user))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    @Override
    public abstract Optional<T> getByRecord(EntityRef recordRef);

    private T fromJson(File file) {
        try {
            return objectMapper.readValue(file.getFileVersion().getBytes(), typeParameterClass);
        } catch (IOException e) {
            throw new RuntimeException("Failed convert File to DashboardDTO", e);
        }
    }

    protected byte[] toJson(T entity) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectMapper.writeValue(output, entity);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed convert entity <%s> to json", typeParameterClass), e);
        }
    }
}
