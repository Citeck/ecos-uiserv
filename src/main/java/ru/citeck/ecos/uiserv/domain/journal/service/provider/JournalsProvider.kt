package ru.citeck.ecos.uiserv.domain.journal.service.provider

import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef

interface JournalsProvider {

    fun getJournalById(id: String): EntityWithMeta<JournalDef>?

    fun getType(): String
}
