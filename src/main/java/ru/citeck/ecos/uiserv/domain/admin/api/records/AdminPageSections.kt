package ru.citeck.ecos.uiserv.domain.admin.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.admin.api.records.service.AdminSecGroupService
import ru.citeck.ecos.uiserv.domain.journal.service.JournalServiceImpl

@Component
class AdminPageSections(
    private val adminSecGroupService: AdminSecGroupService,
    private val journalService: JournalServiceImpl
) : AbstractRecordsDao(), RecordsQueryDao {

    companion object {
        const val JOURNAL_NAME = "journal"
    }

    override fun getId() = "admin-page-section"

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {

        if (recsQuery.language != "groups") {
            return null
        }
        val listSecGroup = adminSecGroupService.all ?: emptyList()
        val result = listSecGroup
            .map { (_, name, _, sections) ->
                PageSectionGroup(
                    name,
                    sections
                        .map { (name1, type, config) ->
                            var pageSecName = name1
                            if (MLText.EMPTY == name1) {
                                if (JOURNAL_NAME.equals(type, ignoreCase = true)) {
                                    val journal = journalService.getJournalById(
                                        config.getAs(kotlin.collections.HashMap::class.java)?.get("journalId")
                                            .toString()
                                    )
                                    pageSecName = journal.journalDef.name
                                }
                            }
                            PageSection(pageSecName, SectionType.valueOf(type), config)
                        }
                )
            }
        return RecsQueryRes(result)
    }

    data class PageSectionGroup(
        val label: MLText,
        val sections: List<PageSection>
    )

    data class PageSection(
        val label: MLText,
        val type: SectionType,
        val config: ObjectData = ObjectData.create()
    )

    enum class SectionType {
        BPM,
        JOURNAL,
        DEV_TOOLS
    }
}
