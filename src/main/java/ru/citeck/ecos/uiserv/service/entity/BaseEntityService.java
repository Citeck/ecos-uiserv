package ru.citeck.ecos.uiserv.service.entity;

import ru.citeck.ecos.records2.RecordRef;
    
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

    List<T> getAllByKey(String type, String key, String user);

    Optional<T> getByKeys(String type, List<String> keys, String user);

    Optional<T> getByRecord(RecordRef recordRef);
}
