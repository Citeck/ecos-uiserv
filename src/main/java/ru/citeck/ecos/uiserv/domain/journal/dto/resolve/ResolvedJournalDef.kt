package ru.citeck.ecos.uiserv.domain.journal.dto.resolve

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.uiserv.domain.journal.api.records.JournalRecordsDao

class ResolvedJournalDef(
    other: JournalRecordsDao.JournalRecord,
    val columnsEval: () -> List<ResolvedColumnDef>
) : JournalRecordsDao.JournalRecord(other) {

    var createVariants: List<CreateVariantDef> = emptyList()

    override fun getColumns(): List<Any> {
        return columnsEval.invoke()
    }

    override fun toNonDefaultJson(): Any {
        val data = ObjectData.create(journalDef)
        data.set("createVariants", createVariants)
        data.set("columns", getColumns())
        return data
    }
}
