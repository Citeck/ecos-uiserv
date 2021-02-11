package ru.citeck.ecos.uiserv.domain.journal.dto.resolve

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.uiserv.domain.journal.api.records.JournalRecordsDao

class ResolvedJournalDef(
    other: JournalRecordsDao.JournalRecord,
    val columns: List<ResolvedColumnDef>
) : JournalRecordsDao.JournalRecord(other) {

    var sourceId: String = ""
    var createVariants: List<CreateVariantDef> = emptyList()

    override fun toJson(): Any {
        val data = ObjectData.create(journalDef)
        data.set("sourceId", sourceId)
        data.set("createVariants", createVariants)
        data.set("columns", columns)
        return data
    }
}
