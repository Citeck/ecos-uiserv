package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValueCtx
import ru.citeck.ecos.records3.record.op.query.dto.query.Consistency
import ru.citeck.ecos.records3.record.op.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import javax.annotation.PostConstruct

@Component
class JournalsTotalCountMixin(
    val recordsService: RecordsService,
    val resolvedJournalRecordsDao: ResolvedJournalRecordsDao
) : AttMixin {

    companion object {
        private const val ATT = "totalCount"
        private val PROVIDED_ATTS = listOf(ATT)
    }

    @PostConstruct
    fun init() {
        resolvedJournalRecordsDao.addAttributesMixin(this)
    }

    override fun getAtt(path: String, value: AttValueCtx): Any? {

        val predicate = value.getAtt("predicate").getAs(Predicate::class.java)

        if (predicate == null || predicate == VoidPredicate.INSTANCE) {
            return 0
        }

        val queryData = value.getAtt("queryData").asObjectData()
        val sourceId = value.getAtt("sourceId").asText()

        val qBuilder = RecordsQuery.create()
            .withSourceId(sourceId)

        if (queryData.size() > 0) {

            qBuilder.withLanguage(PredicateService.LANGUAGE_PREDICATE + "-with-data")

            val query = ObjectData.create()
            query.set("data", queryData)
            query.set("predicate", predicate)

            qBuilder.withQuery(query)
        } else {

            qBuilder.withLanguage(PredicateService.LANGUAGE_PREDICATE)
            qBuilder.withQuery(predicate)
        }

        qBuilder.withConsistency(Consistency.EVENTUAL)
        qBuilder.withPage(QueryPage.create { withMaxItems(1) })

        return recordsService.query(qBuilder.build()).getTotalCount()
    }

    override fun getProvidedAtts() = PROVIDED_ATTS
}
