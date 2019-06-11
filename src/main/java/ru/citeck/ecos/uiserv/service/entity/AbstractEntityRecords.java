package ru.citeck.ecos.uiserv.service.entity;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.CrudRecordsDAO;
import ru.citeck.ecos.uiserv.domain.EntityDTO;
import ru.citeck.ecos.uiserv.service.RecordNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractEntityRecords extends CrudRecordsDAO<EntityDTO> {

    protected BaseEntityService<EntityDTO> entityService;

    @Override
    public List<EntityDTO> getValuesToMutate(List<RecordRef> records) {
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

    protected abstract EntityDTO getEmpty();

    @Override
    public RecordsMutResult save(List<EntityDTO> values) {
        RecordsMutResult recordsMutResult = new RecordsMutResult();
        values.forEach(entityDTO -> {
            EntityDTO saved;

            if (StringUtils.isBlank(entityDTO.getId())) {
                saved = entityService.create(entityDTO);
            } else {
                saved = entityService.update(entityDTO);
            }

            RecordMeta recordMeta = new RecordMeta(saved.getId());
            recordsMutResult.addRecord(recordMeta);
        });
        return recordsMutResult;
    }

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
    public List<EntityDTO> getMetaValues(List<RecordRef> records) {
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
    public RecordsQueryResult<EntityDTO> getMetaValues(RecordsQuery recordsQuery) {
        String language = recordsQuery.getLanguage();
        if (StringUtils.isNotBlank(language)) {
            throw new IllegalArgumentException("This records source does not support query via language");
        }

        RecordsQueryResult<EntityDTO> result = new RecordsQueryResult<>();
        EntityQuery query = recordsQuery.getQuery(EntityQuery.class);
        Optional<EntityDTO> entityDTO = Optional.empty();

        if (StringUtils.isNotBlank(query.key)) {
            entityDTO = entityService.getByKeys(Arrays.stream(query.key.split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList()));
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

    private static class EntityQuery {
        @Getter
        @Setter
        private String key;

        @Getter
        @Setter
        private RecordRef record;
    }

}
