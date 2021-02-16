package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.uiserv.domain.journal.dto.ColumnFormatterDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef

@Component
class ColumnFormatterResolver {

    fun resolve(column: JournalColumnDef.Builder, typeAtt: AttributeDef?) {

        if (column.formatter.type.isNotBlank()) {
            return
        }
        val columnType = column.type ?: return

        when (columnType) {
            AttributeType.ASSOC -> {
                column.withFormatter(ColumnFormatterDef.create { withType("assoc") })
            }
            else -> {
                //do nothing
            }
        }
    }
}
