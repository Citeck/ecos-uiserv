package ru.citeck.ecos.uiserv.app.common.service;

import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
public interface BaseEntityService<T> {

    T create(T entity);

    T update(T entity);

    void delete(String id);

    Optional<T> getById(String id);

    Optional<T> getByKey(String type, String key, String user);

    List<T> getAll();

    List<T> getAllByKey(String type, String key, String user);

    Optional<T> getByKeys(String type, List<String> keys, String user);

    Optional<T> getByRecord(EntityRef recordRef);
}
