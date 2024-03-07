package ru.citeck.ecos.uiserv.app.common.api.records;

import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsCrudDao;
import ru.citeck.ecos.uiserv.app.common.service.EntityDto;
import ru.citeck.ecos.uiserv.app.common.exception.RecordNotFoundException;
import ru.citeck.ecos.uiserv.app.common.service.BaseEntityService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.stream.Collectors;

@Deprecated
public abstract class AbstractEntityRecords<T extends EntityDto> extends LocalRecordsCrudDao<T> {

    protected BaseEntityService<T> entityService;

    @Override
    public List<T> getValuesToMutate(List<EntityRef> records) {
        return records.stream()
            .map(EntityRef::getLocalId)
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
                entityService.delete(recordRef.getLocalId());
                resultRecords.add(new RecordMeta(recordRef));
            });

        RecordsDelResult result = new RecordsDelResult();
        result.setRecords(resultRecords);
        return result;
    }

    @Override
    public List<T> getLocalRecordsMeta(@NotNull List<EntityRef> records, @NotNull MetaField metaField) {
        return records.stream()
            .map(EntityRef::getLocalId)
            .map(id -> Optional.of(id)
                .filter(str -> !str.isEmpty())
                .map(x -> entityService.getById(x)
                    .orElseThrow(() -> new RecordNotFoundException(String.format("Entity with id <%s> not found!",
                        id))))
                .orElseGet(this::getEmpty))
            .collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<T> queryLocalRecords(@NotNull RecordsQuery recordsQuery, @NotNull MetaField field) {

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
