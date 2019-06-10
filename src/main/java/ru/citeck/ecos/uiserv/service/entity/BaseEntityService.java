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

    Optional<T> getByKey(String key);

    Optional<T> getByKeys(List<String> keys);

    Optional<T> getByRecord(RecordRef recordRef);

}
