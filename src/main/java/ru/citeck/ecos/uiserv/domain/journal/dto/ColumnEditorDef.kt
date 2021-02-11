package ru.citeck.ecos.uiserv.domain.journal.dto

import ru.citeck.ecos.commons.data.ObjectData

data class ColumnEditorDef(
    val type: String,
    val config: ObjectData
) {
    companion object {
        @JvmField
        val EMPTY = ColumnEditorDef("", ObjectData.create())
    }
}
