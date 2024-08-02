package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.dto.ColumnFormatterDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry

@Component
class ColumnFormatterResolver(
    val ecosTypeService: EcosTypeService,
    val ecosTypesRegistry: EcosTypesRegistry
) {
    companion object {
        private val TYPE_CASE = ModelUtils.getTypeRef("case")
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
                    val typeInfo = ecosTypeService.getTypeInfo(EntityRef.valueOf(typeRef))
                    if (ecosTypesRegistry.getParents(typeInfo).contains(TYPE_CASE)) {
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
                // do nothing
            }
        }

        if (column.formatter.type.isBlank()) {
            if (column.id == "cm:title" || column.id == "_disp") {
                column.withFormatter(ColumnFormatterDef.create { withType("link") })
            }
        }
    }
}
