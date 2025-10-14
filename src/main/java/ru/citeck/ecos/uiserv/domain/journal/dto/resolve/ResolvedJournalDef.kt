package ru.citeck.ecos.uiserv.domain.journal.dto.resolve

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.uiserv.domain.journal.api.records.JournalRecordsDao
import ru.citeck.ecos.uiserv.domain.journal.api.records.ResolvedJournalRecordsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ResolvedJournalDef(
    other: JournalRecordsDao.JournalRecord,
    workspaceService: WorkspaceService,
    val columnsEval: () -> List<ResolvedColumnDef>
) : JournalRecordsDao.JournalRecord(other, workspaceService) {

    var createVariants: List<CreateVariantDef> = emptyList()

    @AttName(ScalarType.ID_SCHEMA)
    override fun getRef(): EntityRef {
        return super.getRef().withSourceId(ResolvedJournalRecordsDao.ID)
    }

    override fun getLocalId(): String {
        return super.getRef().getLocalId()
    }

    override fun getModuleId(): String {
        return getLocalId()
    }

    override fun getColumns(): List<Any> {
        return columnsEval.invoke()
    }

    override fun toNonDefaultJson(): Any {
        val data = ObjectData.create(journalDef)
        data["id"] = getLocalId()
        data["createVariants"] = createVariants
        data["columns"] = getColumns()
        return data
    }
}
