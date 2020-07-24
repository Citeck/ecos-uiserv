package ru.citeck.ecos.uiserv.domain.userconfig.api.records;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.uiserv.domain.userconfig.dto.UserConfigurationDto;
import ru.citeck.ecos.uiserv.domain.userconfig.service.UserConfigurationsService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserConfigurationsRecords extends LocalRecordsDao
    implements MutableRecordsLocalDao<UserConfigurationDto>, LocalRecordsMetaDao<UserConfigurationDto> {

    public static final String ID = "user-conf";

    private final UserConfigurationsService userConfigurationsService;

    public UserConfigurationsRecords(UserConfigurationsService userConfigurationsService) {
        setId(ID);
        this.userConfigurationsService = userConfigurationsService;
    }

    @Override
    public RecordsMutResult save(@NotNull List<UserConfigurationDto> values) {
        RecordsMutResult result = new RecordsMutResult();
        for (UserConfigurationDto value : values) {
            UserConfigurationDto saved = userConfigurationsService.save(value);
            result.addRecord(new RecordMeta(saved.getId()));
        }
        return result;
    }

    @Override
    public RecordsDelResult delete(@NotNull RecordsDeletion deletion) {
        throw new UnsupportedOperationException("Delete operation for user configurations not supported");
    }

    @Override
    public List<UserConfigurationDto> getValuesToMutate(@NotNull List<RecordRef> records) {
        return getRecordsFromRecordRefs(records);
    }

    @Override
    public List<UserConfigurationDto> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
        return getRecordsFromRecordRefs(records);
    }

    private List<UserConfigurationDto> getRecordsFromRecordRefs(Collection<RecordRef> records) {
        return records.stream()
            .map(RecordRef::getId)
            .map(extId ->
                Optional.ofNullable(userConfigurationsService.findByExternalId(extId))
                    .orElseGet(UserConfigurationDto::new)
            ).collect(Collectors.toList());
    }
}
