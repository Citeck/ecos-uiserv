package ru.citeck.ecos.uiserv.domain.journal.dto

import ru.citeck.ecos.commons.data.ObjectData

data class JournalComputedDef(
    val id: String,
    val type: String,
    val config: ObjectData
)
