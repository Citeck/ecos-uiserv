package ru.citeck.ecos.uiserv.domain.admin.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery

@Component
class AdminPageSections : AbstractRecordsDao(), RecordsQueryDao {

    override fun getId() = "admin-page-section"

    override fun queryRecords(query: RecordsQuery): RecsQueryRes<*>? {

        if (query.language != "groups") {
            return null
        }

        val result = listOf(
            PageSectionGroup(
                MLText("Управление системой"),
                listOf(
                    PageSection(
                        MLText("Инструменты разработчика"),
                        SectionType.DEV_TOOLS
                    ),
                    PageSection(
                        MLText("Приложения ECOS"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-apps"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Артифакты ECOS"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-artifacts"
                        }""".trimIndent())
                    )
                )
            ),
            PageSectionGroup(
                MLText("Управление процессами"),
                listOf(
                    PageSection(
                        MLText("Модели бизнес-процессов"),
                        SectionType.BPM
                    )
                )
            ),
            PageSectionGroup(
                MLText("Модель"),
                listOf(
                    PageSection(
                        MLText("Типы данных"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-types"
                        }""".trimIndent())
                    )
                )
            ),
            PageSectionGroup(
                MLText("Конфигурация UI"),
                listOf(
                    PageSection(
                        MLText("Журналы"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-journals"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Формы"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-forms"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Действия"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ui-actions"
                        }""".trimIndent())
                    )
                )
            ),
            PageSectionGroup(
                MLText("Конфигурация уведомлений"),
                listOf(
                    PageSection(
                        MLText("Файлы уведомлений"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "notification-files"
                        }""".trimIndent())
                    )
                )
            )
        )

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
