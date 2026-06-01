package ru.citeck.ecos.uiserv.domain.board.cardorder.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Board attributes read from the `rboard` (resolved board) records source.
 * Columns are read as raw JSON and bound to [ColumnDef] by field name.
 */
class BoardInfo(
    @AttName("typeRef?id") val typeRef: EntityRef = EntityRef.EMPTY,
    @AttName("journalRef?id") val journalRef: EntityRef = EntityRef.EMPTY,
    @AttName("columns[]?json") val columns: List<ColumnDef> = emptyList()
) {
    class ColumnDef(
        val id: String = "",
        val name: MLText = MLText.EMPTY,
        val hasSum: Boolean = false,
        val sumAtt: String = "",
        val hideOldItems: Boolean = false,
        val hideItemsOlderThan: String? = null
    )
}
