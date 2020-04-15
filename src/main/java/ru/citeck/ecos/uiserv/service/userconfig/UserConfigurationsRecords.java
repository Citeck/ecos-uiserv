package ru.citeck.ecos.uiserv.service.userconfig;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.uiserv.dto.config.UserConfigurationDto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserConfigurationsRecords extends LocalRecordsDAO
    implements MutableRecordsLocalDAO<UserConfigurationDto>, LocalRecordsMetaDAO<UserConfigurationDto> {

    public static final String ID = "user-conf";

    private final UserConfigurationsService userConfigurationsService;

    public UserConfigurationsRecords(UserConfigurationsService userConfigurationsService) {
        setId(ID);
        this.userConfigurationsService = userConfigurationsService;
    }

    @Override
    public RecordsMutResult save(List<UserConfigurationDto> values) {
        RecordsMutResult result = new RecordsMutResult();
        for (UserConfigurationDto value : values) {
            UserConfigurationDto saved = userConfigurationsService.save(value);
            result.addRecord(new RecordMeta(saved.getId()));
        }
        return result;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        throw new UnsupportedOperationException("Delete operation for user configurations not supported");
    }

    @Override
    public List<UserConfigurationDto> getValuesToMutate(List<RecordRef> records) {
        return getRecordsFromRecordRefs(records);
    }

    @Override
    public List<UserConfigurationDto> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
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
