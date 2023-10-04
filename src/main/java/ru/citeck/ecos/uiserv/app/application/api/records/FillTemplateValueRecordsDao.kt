package ru.citeck.ecos.uiserv.app.application.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.utils.TmplUtils
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class FillTemplateValueRecordsDao : AbstractRecordsDao(), RecordsQueryDao {

    companion object {
        const val ID = "fill-template-value"
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        if (recsQuery.language != "") {
            return null
        }
        val data = recsQuery.getQuery(QueryData::class.java)

        val attsToLoad = TmplUtils.getAtts(data.value)
        if (attsToLoad.isEmpty()) {
            return data.value
        }
        val attsResult = RequestContext.doWithAtts(mapOf("context" to data.context)) { _ ->
            recordsService.getAtts(data.record, attsToLoad).getAtts()
        }
        return TmplUtils.applyAtts(data.value, attsResult)
    }

    override fun getId(): String {
        return ID
    }

    class QueryData(
        val record: EntityRef = EntityRef.EMPTY,
        val context: DataValue = DataValue.createObj(),
        val value: DataValue = DataValue.NULL
    )
}
