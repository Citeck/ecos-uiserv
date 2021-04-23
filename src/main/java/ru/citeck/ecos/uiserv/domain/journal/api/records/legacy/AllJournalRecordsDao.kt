package ru.citeck.ecos.uiserv.domain.journal.api.records.legacy

import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.journal.api.records.ResolvedJournalRecordsDao

@Slf4j
@Component
class AllJournalRecordsDao(
    private val resolvedJournalRecordsDao: ResolvedJournalRecordsDao
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao {

    override fun getId() = "journal_all"

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        return resolvedJournalRecordsDao.queryRecords(recsQuery)
    }

    override fun getRecordAtts(recordId: String): Any? {
        return resolvedJournalRecordsDao.getRecordAtts(recordId)
    }
}
