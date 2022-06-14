package ru.citeck.ecos.uiserv.domain.action.dto

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName

data class ExecForQueryConfig(
    val execAsForRecords: Boolean? = null,
    /**
     * Workaround to avoid simplification of data for DTO with one field
     * This problem will fix in next release and this field won't be required
     */
    @JsonIgnore
    @AttName(RecordConstants.ATT_NULL)
    val nullField: String? = null
) {
    companion object {
        @JvmField
        val EMPTY = ExecForQueryConfig()
    }
}
