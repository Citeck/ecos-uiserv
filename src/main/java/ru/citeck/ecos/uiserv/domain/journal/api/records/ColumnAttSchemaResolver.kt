package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef

@Component
class ColumnAttSchemaResolver {

    fun resolve(column: JournalColumnDef.Builder, typeAtt: AttributeDef?) {

        if (column.attSchema.isNotBlank()) {
            return
        }
        if (column.editor.type == "select") {
            column.attSchema = "{value:?str,disp:?disp}"
        }
    }
}
