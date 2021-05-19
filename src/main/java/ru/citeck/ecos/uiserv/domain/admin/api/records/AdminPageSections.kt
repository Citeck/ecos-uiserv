package ru.citeck.ecos.uiserv.domain.admin.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.admin.api.records.service.AdminSecGroupService
import java.util.stream.Collectors

@Component
class AdminPageSections(
    private val adminSecGroupService: AdminSecGroupService
) : AbstractRecordsDao(), RecordsQueryDao {

    override fun getId() = "admin-page-section"

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {

        if (recsQuery.language != "groups") {
            return null
        }
        val listSecGroup = adminSecGroupService.all ?: emptyList()

        val result = listSecGroup.stream()
            .map { (_, name, _, sections) ->
                PageSectionGroup(
                    name,
                    sections.stream()
                        .map { (name1, type, config) ->
                            PageSection(name1, SectionType.valueOf(type), config)
                        }
                        .collect(Collectors.toList())
                )
            }
            .collect(Collectors.toList())
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
