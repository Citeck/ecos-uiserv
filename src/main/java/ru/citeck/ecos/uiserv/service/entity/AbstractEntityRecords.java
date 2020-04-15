package ru.citeck.ecos.uiserv.service.entity;

import lombok.Data;
import org.apache.commons.lang.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.CrudRecordsDAO;
import ru.citeck.ecos.uiserv.dto.EntityDto;
import ru.citeck.ecos.uiserv.service.RecordNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractEntityRecords<T extends EntityDto> extends CrudRecordsDAO<T> {

    protected BaseEntityService<T> entityService;

    @Override
    public List<T> getValuesToMutate(List<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(id ->
                Optional.of(id)
                    .filter(str -> !str.isEmpty())
                    .map(x -> entityService.getById(x)
                        .orElseThrow(() -> new RecordNotFoundException(String.format("Entity with id <%s> not found!",
                            id))))
                    .orElseGet(this::getEmpty))
            .collect(Collectors.toList());
    }

    protected abstract T getEmpty();

    public abstract RecordsMutResult save(List<T> values);

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        List<RecordMeta> resultRecords = new ArrayList<>();
        deletion.getRecords()
            .forEach(recordRef -> {
                entityService.delete(recordRef.getId());
                resultRecords.add(new RecordMeta(recordRef));
            });

        RecordsDelResult result = new RecordsDelResult();
        result.setRecords(resultRecords);
        return result;
    }

    @Override
    public List<T> getMetaValues(List<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(id -> Optional.of(id)
                .filter(str -> !str.isEmpty())
                .map(x -> entityService.getById(x)
                    .orElseThrow(() -> new RecordNotFoundException(String.format("Entity with id <%s> not found!",
                        id))))
                .orElseGet(this::getEmpty))
            .collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<T> getMetaValues(RecordsQuery recordsQuery) {

        String language = recordsQuery.getLanguage();
        if (StringUtils.isNotBlank(language)) {
            throw new IllegalArgumentException("This records source does not support query via language");
        }

        RecordsQueryResult<T> result = new RecordsQueryResult<>();
        EntityQuery query = recordsQuery.getQuery(EntityQuery.class);
        if (query.key == null) {
            query.key = "DEFAULT";
        }
        if (StringUtils.isEmpty(query.user)) {
            query.user = null;
        }
        Optional<T> entityDTO = Optional.empty();

        if (StringUtils.isNotBlank(query.key)) {
            List<String> keys = Arrays.stream(query.key.split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
            entityDTO = entityService.getByKeys(query.type, keys, query.user);
            if (!entityDTO.isPresent()) {
                entityDTO = entityService.getByKeys(query.type, keys, null);
            }
        } else if (query.record != null) {
            entityDTO = entityService.getByRecord(query.record);
        }

        entityDTO
            .map(Collections::singletonList)
            .ifPresent(list -> {
                result.setRecords(list);
                result.setTotalCount(list.size());
            });

        return result;
    }

    @Data
    private static class EntityQuery {
        private String key;
        private String type;
        private String user;
        private RecordRef record;
    }
}
