package ru.citeck.ecos.uiserv.service.icon;

import lombok.Data;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.uiserv.service.icon.dto.IconDto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class IconRecords extends LocalRecordsDao
    implements LocalRecordsQueryWithMetaDao<IconDto>,
    LocalRecordsMetaDao<IconDto>,
    MutableRecordsLocalDao<IconDto> {

    private static final String ID = "icon";
    private final IconService iconService;

    public IconRecords(IconService iconService) {
        setId(ID);
        this.iconService = iconService;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        RecordsDelResult result = new RecordsDelResult();
        for (RecordRef record : deletion.getRecords()) {
            iconService.deleteById(record.getId());
            result.addRecord(new RecordMeta(record));
        }
        return result;
    }

    @Override
    public RecordsQueryResult<IconDto> queryLocalRecords(RecordsQuery recordsQuery, MetaField field) {
        RecordsQueryResult<IconDto> result = new RecordsQueryResult<>();

        TypeQuery typeQuery = recordsQuery.getQuery(TypeQuery.class);
        if (typeQuery == null || typeQuery.family == null && typeQuery.type == null) {
            result.setRecords(iconService.findAll());
            return result;
        }

        if (typeQuery.family != null) {
            if (typeQuery.type != null) {
                result.setRecords(iconService.findAllByFamilyAndType(typeQuery.family, typeQuery.type));
            } else {
                result.setRecords(iconService.findAllByFamily(typeQuery.family));
            }
        } else {
            result.setRecords(iconService.findAllByFamilyAndType("", typeQuery.type));
        }

        return result;
    }

    @Override
    public List<IconDto> getValuesToMutate(List<RecordRef> records) {
        return getLocalRecordsMeta(records, null);
    }

    @Override
    public RecordsMutResult save(List<IconDto> values) {
        RecordsMutResult result = new RecordsMutResult();

        List<RecordMeta> savedList = values.stream()
            .map(iconService::save)
            .map(IconDto::getId)
            .map(RecordMeta::new)
            .collect(Collectors.toList());
        result.setRecords(savedList);

        return result;
    }

    @Override
    public List<IconDto> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream()
            .map(RecordRef::getId)
            .map(iconService::findById)
            .map(opt -> opt.orElseGet(IconDto::new))
            .collect(Collectors.toList());
    }

    @Data
    private static class TypeQuery {
        private String type;
        private String family;
    }
}
