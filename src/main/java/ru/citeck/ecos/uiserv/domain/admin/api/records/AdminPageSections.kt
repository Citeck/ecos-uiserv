package ru.citeck.ecos.uiserv.domain.admin.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

@Component
class AdminPageSections : AbstractRecordsDao(), RecordsQueryDao {

    override fun getId() = "admin-page-section"

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {

        if (recsQuery.language != "groups") {
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
                        MLText("Артефакты ECOS"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-artifacts"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Патчи артифактов"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-module-patches"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Конфигурация"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecosConfig"
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
                    ),
                    PageSection(
                        MLText("CMMN Сценарии"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "cmmn-process-def"
                        }""".trimIndent())
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
                    ),
                    PageSection(
                        MLText("Шаблоны нумерации"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "number-templates"
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
                    ),
                    PageSection(
                        MLText("Дашборды"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-dashboards"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Темы"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-theme"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Локализация"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-i18n"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Меню"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-menus"
                        }""".trimIndent())
                    )
                )
            ),
            PageSectionGroup(
                MLText("Конфигурация уведомлений"),
                listOf(
                    PageSection(
                        MLText("Шаблоны уведомлений"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "notification-templates"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Файлы уведомлений"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "notification-files"
                        }""".trimIndent())
                    )
                )
            ),
            PageSectionGroup(
                MLText("Интеграция"),
                listOf(
                    PageSection(
                        MLText("Синхронизации"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-sync"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Источники данных"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-data-sources"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Credentials"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-credentials"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Конфигурация ящиков ЭДО"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "edi-box"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("OSGI Пакеты"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "ecos-osgi-bundles"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Конфигурация импорта файлов"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "file-import-config"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Файлы для импорта"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "file-import-task"
                        }""".trimIndent())
                    ),
                    PageSection(
                        MLText("Элементы файлового импорта"),
                        SectionType.JOURNAL,
                        ObjectData.create("""{
                            "journalId": "file-import-task-item"
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
