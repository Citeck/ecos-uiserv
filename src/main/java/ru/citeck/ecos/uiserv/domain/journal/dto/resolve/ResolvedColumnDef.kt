package ru.citeck.ecos.uiserv.domain.journal.dto.resolve

import ecos.com.fasterxml.jackson210.annotation.JsonValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef

class ResolvedColumnDef(
    @AttName("...")
    val column: JournalColumnDef,
    val attConfig: ObjectData
) {

    @JsonValue
    fun toJson(): ObjectData {
        val result = ObjectData.create(column)
        result.set("attConfig", attConfig)
        return result
    }
}
