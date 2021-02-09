package ru.citeck.ecos.uiserv.domain.journal.api.records.legacy

import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.journal.api.records.ResolvedJournalRecordsDao

@Slf4j
@Component
class V1JournalRecordsDao(
    private val resolvedJournalRecordsDao: ResolvedJournalRecordsDao
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao {

    override fun getId() = "journal_v1"

    override fun queryRecords(query: RecordsQuery): Any? {
        return resolvedJournalRecordsDao.queryRecords(query)
    }

    override fun getRecordAtts(record: String): Any? {
        return resolvedJournalRecordsDao.getRecordAtts(record)
    }
}
