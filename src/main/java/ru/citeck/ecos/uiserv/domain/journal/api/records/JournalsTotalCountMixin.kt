package ru.citeck.ecos.uiserv.domain.journal.api.records

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.mixin.AttMixin

@Component
class JournalsTotalCountMixin(
    val recordsService: RecordsService,
    val resolvedJournalRecordsDao: ResolvedJournalRecordsDao,
    val journalsServiceRecordsDao: JournalsServiceRecordsDao
) : AttMixin {

    companion object {
        private const val ATT = "totalCount"
        private val PROVIDED_ATTS = listOf(ATT)
    }

    @PostConstruct
    fun init() {
        resolvedJournalRecordsDao.addAttributesMixin(this)
    }

    override fun getAtt(path: String, value: AttValueCtx): Any? {
        return journalsServiceRecordsDao.getTotalCountForJournal(value.getLocalId(), emptyList())
    }

    override fun getProvidedAtts() = PROVIDED_ATTS
}
