package ru.citeck.ecos.uiserv.domain.journal.dto

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText

data class ColumnOptionDef(
    val label: MLText,
    val value: DataValue
)
