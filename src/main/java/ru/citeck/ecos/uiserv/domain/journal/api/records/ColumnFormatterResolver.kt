package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.dto.ColumnFormatterDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef

@Component
class ColumnFormatterResolver(
    val ecosTypeService: EcosTypeService
) {
    companion object {
        private val TYPE_CASE = RecordRef.valueOf("emodel/type@case")
    }

    fun resolve(column: JournalColumnDef.Builder, typeAtt: AttributeDef?) {

        if (column.formatter.type.isNotBlank()) {
            return
        }
        val columnType = column.type ?: return

        when (columnType) {
            AttributeType.ASSOC -> {
                val typeRef = typeAtt?.config?.get("typeRef")?.asText() ?: ""
                if (typeRef.isNotBlank()) {
                    val typeInfo = ecosTypeService.getTypeInfo(RecordRef.valueOf(typeRef))
                    if (typeInfo?.parents?.contains(TYPE_CASE) == true) {
                        column.withFormatter(ColumnFormatterDef.create { withType("assoc") })
                    }
                }
            }
            AttributeType.DATE -> {
                column.withFormatter(ColumnFormatterDef.create { withType("date") })
            }
            AttributeType.DATETIME -> {
                column.withFormatter(ColumnFormatterDef.create { withType("datetime") })
            }
            else -> {
                //do nothing
            }
        }
    }
}
