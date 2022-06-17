package ru.citeck.ecos.uiserv.domain.action.dto

data class ExecForQueryConfig(
    val execAsForRecords: Boolean? = null
) {
    companion object {
        @JvmField
        val EMPTY = ExecForQueryConfig()
    }
}
