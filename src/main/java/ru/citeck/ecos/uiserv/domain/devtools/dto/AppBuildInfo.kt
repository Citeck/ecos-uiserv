package ru.citeck.ecos.uiserv.domain.devtools.dto

import ru.citeck.ecos.commons.data.ObjectData

data class AppBuildInfo(
    val id: String,
    val label: String,
    val description: String,
    val info: ObjectData
)
