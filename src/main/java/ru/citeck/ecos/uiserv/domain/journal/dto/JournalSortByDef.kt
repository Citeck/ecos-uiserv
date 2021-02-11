package ru.citeck.ecos.uiserv.domain.journal.dto

data class JournalSortByDef(
    val attribute: String,
    val ascending: Boolean = false
)
