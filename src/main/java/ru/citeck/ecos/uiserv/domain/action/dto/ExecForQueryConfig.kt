package ru.citeck.ecos.uiserv.domain.action.dto

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName

data class ExecForQueryConfig(
    val execAsForRecords: Boolean? = null
) {
    companion object {
        @JvmField
        val EMPTY = ExecForQueryConfig()
    }
}
