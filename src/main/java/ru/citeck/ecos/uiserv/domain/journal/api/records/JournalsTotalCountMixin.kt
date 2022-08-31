package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.dao.query.dto.query.Consistency
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext
import javax.annotation.PostConstruct

@Component
class JournalsTotalCountMixin(
    val recordsService: RecordsService,
    val resolvedJournalRecordsDao: ResolvedJournalRecordsDao,
    val ecosWebAppContext: EcosWebAppContext
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
        val sourceId = value.getAtt("sourceId").asText()

        if (isAlfrescoNodeSourceIdWithEmptyPredicate(sourceId, predicate)) {
            return 0
        }

        if (!ecosWebAppContext.getWebAppsApi().isAppAvailable(getAppName(sourceId))) {
            return null
        }

        val qBuilder = RecordsQuery.create()
            .withSourceId(sourceId)
        val queryData = value.getAtt("queryData").asObjectData()

        if (queryData.size() > 0) {

            qBuilder.withLanguage(PredicateService.LANGUAGE_PREDICATE + "-with-data")

            val query = ObjectData.create()
            query["data"] = queryData
            query["predicate"] = predicate

            qBuilder.withQuery(query)
        } else {

            qBuilder.withLanguage(PredicateService.LANGUAGE_PREDICATE)
            qBuilder.withQuery(predicate)
        }

        qBuilder.withConsistency(Consistency.EVENTUAL)
        qBuilder.withPage(QueryPage.create { withMaxItems(0) })

        return recordsService.query(qBuilder.build()).getTotalCount()
    }

    override fun getProvidedAtts() = PROVIDED_ATTS

    private fun getAppName(sourceId: String): String {
        if (sourceId.indexOf(RecordRef.APP_NAME_DELIMITER) == -1) {
            return AppName.ALFRESCO
        }
        return sourceId.substringBefore(RecordRef.APP_NAME_DELIMITER)
    }

    private fun isAlfrescoNodeSourceIdWithEmptyPredicate(sourceId: String, predicate: Predicate?): Boolean {
        return sourceId == AppName.ALFRESCO + RecordRef.APP_NAME_DELIMITER &&
            (predicate == null || predicate == VoidPredicate.INSTANCE)
    }
}
