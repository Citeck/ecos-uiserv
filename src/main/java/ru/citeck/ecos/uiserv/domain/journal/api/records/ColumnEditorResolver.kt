package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.attributes.dto.AttOptionDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.model.lib.status.constants.StatusConstants
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.dto.ColumnEditorDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class ColumnEditorResolver(
    val ecosTypeService: EcosTypeService
) {

    fun resolve(column: JournalColumnDef.Builder, typeAtt: AttributeDef?) {

        if (column.editor.type.isNotBlank()) {
            return
        }

        if (column.id == StatusConstants.ATT_STATUS) {
            column.withEditor(ColumnEditorDef("select", ObjectData.create()))
            return
        }

        val attOptions = emptyList<AttOptionDef>() // typeAtt?.options ?: emptyList()
        if (attOptions.isNotEmpty()) {
            val config = ObjectData.create()
            config.set(
                "options",
                attOptions.map {
                    val option = ObjectData.create()
                    option.set("label", MLText.getClosestValue(it.label, I18nContext.getLocale()))
                    option.set("value", it.value)
                    option
                }
            )
            column.withEditor(ColumnEditorDef("select", config))
        }

        val columnType = column.type ?: return

        when (columnType) {
            AttributeType.ASSOC -> {
                column.withEditor(resolveEditorForAssoc(typeAtt))
            }
            AttributeType.TEXT -> {
                column.withEditor(resolveEditorForText())
            }
            AttributeType.BOOLEAN -> {
                column.withEditor(resolveEditorForBoolean())
            }
            AttributeType.NUMBER -> {
                column.withEditor(resolveEditorForNumber())
            }
            AttributeType.CONTENT -> {
                column.withEditor(resolveEditorForContent())
            }
            AttributeType.AUTHORITY,
            AttributeType.AUTHORITY_GROUP,
            AttributeType.PERSON -> {
                column.withEditor(resolveEditorForAuthority(columnType))
            }
            AttributeType.DATE,
            AttributeType.DATETIME -> {
                column.withEditor(resolveEditorForDateTime(columnType))
            }
            AttributeType.MLTEXT -> {
                column.withEditor(resolveEditorForMLText())
            }
            else -> {}
        }
    }

    private fun resolveEditorForContent(): ColumnEditorDef {
        return ColumnEditorDef("content", ObjectData.create())
    }

    private fun resolveEditorForBoolean(): ColumnEditorDef {
        return ColumnEditorDef("boolean", ObjectData.create())
    }

    private fun resolveEditorForNumber(): ColumnEditorDef {
        return ColumnEditorDef("number", ObjectData.create())
    }

    private fun resolveEditorForMLText(): ColumnEditorDef {
        return ColumnEditorDef("text", ObjectData.create())
    }

    private fun resolveEditorForDateTime(attType: AttributeType): ColumnEditorDef {

        val editorType = when (attType) {
            AttributeType.DATE -> "date"
            else -> "datetime"
        }

        return ColumnEditorDef(editorType, ObjectData.create())
    }

    private fun resolveEditorForAuthority(attType: AttributeType): ColumnEditorDef {

        val config = ObjectData.create()

        config.set(
            "allowedAuthorityTypes",
            when (attType) {
                AttributeType.PERSON -> "USER"
                AttributeType.AUTHORITY_GROUP -> "GROUP"
                else -> "USER,GROUP"
            }
        )

        return ColumnEditorDef("orgstruct", config)
    }

    private fun resolveEditorForText(): ColumnEditorDef {
        return ColumnEditorDef("text", ObjectData.create())
    }

    private fun resolveEditorForAssoc(typeAtt: AttributeDef?): ColumnEditorDef {

        val calculatedJournalId: String? = if (typeAtt != null) {
            val assocTypeRef = EntityRef.valueOf(typeAtt.config.get("typeRef").asText())
            ecosTypeService.getJournalRefByTypeRef(assocTypeRef).getLocalId()
        } else {
            null
        }

        val journalId = if (!calculatedJournalId.isNullOrBlank()) {
            calculatedJournalId
        } else {
            "search"
        }

        val journalEditorConfig = ObjectData.create()
        journalEditorConfig.set("journalId", journalId)

        return ColumnEditorDef("journal", journalEditorConfig)
    }
}
