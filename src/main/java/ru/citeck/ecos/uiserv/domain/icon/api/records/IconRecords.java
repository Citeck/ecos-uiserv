package ru.citeck.ecos.uiserv.domain.icon.api.records;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.uiserv.domain.icon.service.IconService;
import ru.citeck.ecos.uiserv.domain.icon.dto.IconDto;

import java.util.List;

@Component
public class IconRecords extends AbstractRecordsDao
    implements RecordsQueryDao,
    RecordAttsDao,
    RecordMutateDtoDao<IconDto>,
    RecordsDeleteDao {

    public static final String ID = "icon";
    private final IconService iconService;

    public IconRecords(IconService iconService) {
        this.iconService = iconService;
    }

    @NotNull
    @Override
    public List<DelStatus> delete(@NotNull List<String> records) throws Exception {
        records.forEach(iconService::deleteById);
        return records.stream().map(r -> DelStatus.OK).toList();
    }

    @Nullable
    @Override
    public Object queryRecords(@NotNull RecordsQuery recordsQuery) throws Exception {
        RecsQueryRes<IconDto> result = new RecsQueryRes<>();

        TypeQuery typeQuery = recordsQuery.getQuery(TypeQuery.class);
        if (typeQuery.family == null && typeQuery.type == null) {
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
    public IconDto getRecToMutate(@NotNull String recordId) throws Exception {
        return getRecordAtts(recordId);
    }

    @NotNull
    @Override
    public String saveMutatedRec(IconDto iconDto) throws Exception {
        return iconService.save(iconDto).getId();
    }

    @Nullable
    @Override
    public IconDto getRecordAtts(@NotNull String recordId) throws Exception {
        return iconService.findById(recordId).orElseGet(IconDto::new);
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Data
    private static class TypeQuery {
        private String type;
        private String family;
    }
}
