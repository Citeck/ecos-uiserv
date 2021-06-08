package ru.citeck.ecos.uiserv.domain.admin.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.admin.dto.AdminSectionDef
import ru.citeck.ecos.uiserv.domain.admin.dto.AdminSectionsGroupDef
import ru.citeck.ecos.uiserv.domain.admin.service.AdminSectionsGroupService
import ru.citeck.ecos.uiserv.domain.journal.service.JournalServiceImpl

@Component
class AdminSectionsGroupRecordsDao(
    private val adminSectionsGroupService: AdminSectionsGroupService,
    private val journalService: JournalServiceImpl
) : AbstractRecordsDao(), RecordsQueryDao {

    companion object {
        private const val JOURNAL_SECTION_TYPE = "JOURNAL"
    }

    override fun getId() = "admin-page-section"

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        if (recsQuery.language != "groups") {
            return null
        }
        val listSecGroup = adminSectionsGroupService.findAll()
        return listSecGroup.map { group ->
            PageSectionGroup(group.withSections(group.sections.map {
                resolveSection(it)
            }))
        }
    }

    private fun resolveSection(section: AdminSectionDef): AdminSectionDef {

        if (!MLText.isEmpty(section.name) || section.type != JOURNAL_SECTION_TYPE) {
            return section
        }

        val journalId = section.config.get("journalId").asText()
        if (journalId.isBlank()) {
            return section
        }
        val journal = journalService.getJournalById(journalId) ?: return section
        return section.withName(journal.journalDef.name)
    }

    data class PageSectionGroup(
        @AttName("...")
        val group: AdminSectionsGroupDef
    ) {
        fun getLabel(): MLText {
            return group.name
        }
        fun getSections(): List<PageSection> {
            return group.sections.map { PageSection(it) }
        }
    }

    data class PageSection(
        @AttName("...")
        val section: AdminSectionDef
    ) {
        fun getLabel(): MLText {
            return section.name
        }
    }
}
