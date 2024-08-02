package ru.citeck.ecos.uiserv.domain.userconfig.api.records;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.uiserv.domain.userconfig.dto.UserConfigurationDto;
import ru.citeck.ecos.uiserv.domain.userconfig.service.UserConfigurationsService;

import java.util.List;
import java.util.Optional;

@Component
public class UserConfigurationsRecords extends AbstractRecordsDao
    implements RecordMutateDtoDao<UserConfigurationDto>, RecordAttsDao, RecordsDeleteDao {

    public static final String ID = "user-conf";

    private final UserConfigurationsService userConfigurationsService;

    public UserConfigurationsRecords(UserConfigurationsService userConfigurationsService) {
        this.userConfigurationsService = userConfigurationsService;
    }

    @NotNull
    @Override
    public String saveMutatedRec(UserConfigurationDto userConfigurationDto) throws Exception {
        return userConfigurationsService.save(userConfigurationDto).getId();
    }

    @NotNull
    @Override
    public List<DelStatus> delete(@NotNull List<String> list) throws Exception {
        throw new UnsupportedOperationException("Delete operation for user configurations not supported");
    }

    @Override
    public UserConfigurationDto getRecToMutate(@NotNull String recordId) throws Exception {
        return getRecordsFromRecordRefs(recordId);
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {
        return getRecordsFromRecordRefs(recordId);
    }

    private UserConfigurationDto getRecordsFromRecordRefs(String recordId) {
        return Optional.ofNullable(userConfigurationsService.findByExternalId(recordId))
            .orElseGet(UserConfigurationDto::new);
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }
}
