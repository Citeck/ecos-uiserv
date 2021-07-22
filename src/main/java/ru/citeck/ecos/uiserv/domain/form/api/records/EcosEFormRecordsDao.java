package ru.citeck.ecos.uiserv.domain.form.api.records;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;

/**
 * @deprecated use uiserv/form@... instead
 */
@Deprecated
@Component
@RequiredArgsConstructor
public class EcosEFormRecordsDao extends AbstractRecordsDao
    implements RecordsQueryDao, RecordAttsDao {

    public static final String ID = "eform";

    private final EcosFormRecordsDao ecosFormRecordsDao;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String formId) {
        return ecosFormRecordsDao.getRecordAtts(formId);
    }

    @Nullable
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {
        return ecosFormRecordsDao.queryRecords(recordsQuery);
    }
}
