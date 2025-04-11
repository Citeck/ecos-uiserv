package ru.citeck.ecos.uiserv.domain.journal.api.records

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.ctx.EcosContext
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.Consistency
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.api.promise.Promises
import java.time.Duration
import java.util.concurrent.*

@Component
class JournalsServiceRecordsDao(
    private val ecosContext: EcosContext,
    private val ecosWebAppContext: EcosWebAppApi
) : AbstractRecordsDao(), RecordsQueryDao {

    companion object {
        const val LANG_JOURNALS_TOTAL_COUNT = "journals-total-count"

        private val log = KotlinLogging.logger {}
    }

    private val semaphore: Semaphore = Semaphore(30, true)

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        if (recsQuery.language != LANG_JOURNALS_TOTAL_COUNT) {
            return null
        }
        val query = recsQuery.getQueryOrNull(JournalsTotalCountQuery::class.java) ?: return null

        val scopeData = ecosContext.getScopeData()
        val result = LongArray(query.journals.size) { -1L }

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val promises = mutableListOf<Promise<Unit>>()
            query.journals.mapIndexed { idx, journalRef ->
                val futureWrapper = CompletableFutureWrapper<Unit>()
                futureWrapper.future = executor.submit {
                    try {
                        if (semaphore.tryAcquire(15, TimeUnit.SECONDS)) {
                            try {
                                ecosContext.newScope(scopeData).use {
                                    result[idx] = getTotalCountForJournal(
                                        journalRef.getLocalId(),
                                        recsQuery.workspaces
                                    )
                                }
                            } finally {
                                semaphore.release()
                            }
                        }
                    } finally {
                        futureWrapper.complete(Unit)
                    }
                }
                promises.add(Promises.create(futureWrapper))
            }
            try {
                Promises.all(promises).get(Duration.ofSeconds(20))
            } catch (e: TimeoutException) {
                val journalsWithoutTotalCount = promises.mapIndexedNotNull { idx, promise ->
                    if (!promise.isDone()) {
                        promise.cancel(true)
                        query.journals[idx]
                    } else {
                        null
                    }
                }
                if (journalsWithoutTotalCount.isNotEmpty()) {
                    log.warn {
                        "Timeout exception while calculating total count " +
                            "for journals: ${journalsWithoutTotalCount.joinToString()}"
                    }
                }
                Unit
            }
        }
        return JournalsTotalCountResp(result.toList())
    }

    fun getTotalCountForJournal(journalId: String, workspaces: List<String>): Long {
        return try {
            getTotalCountForJournalImpl(journalId, workspaces)
        } catch (e: Throwable) {
            log.error(e) { "Error querying totalCount for journal '$journalId'" }
            -1L
        }
    }

    private fun getTotalCountForJournalImpl(journalId: String, workspaces: List<String>): Long {

        val journalAtts = recordsService.getAtts(
            EntityRef.create("rjournal", journalId),
            TotalCountJournalAtts::class.java
        )

        val predicate = journalAtts.predicate ?: return 0L
        val sourceId = journalAtts.sourceId
        if (sourceId.isNullOrBlank()) {
            return 0
        }

        if (isAlfrescoNodeSourceIdWithEmptyPredicate(sourceId, predicate)) {
            return 0
        }

        if (!ecosWebAppContext.getRemoteWebAppsApi().isAppAvailable(getAppName(sourceId))) {
            return -1
        }

        val qBuilder = RecordsQuery.create()
            .withSourceId(sourceId)

        if (workspaces.isNotEmpty()) {
            qBuilder.withWorkspaces(workspaces)
        }
        val queryData = journalAtts.queryData ?: ObjectData.create()

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

    override fun getId(): String {
        return "journals-service"
    }

    private fun getAppName(sourceId: String): String {
        if (sourceId.indexOf(EntityRef.APP_NAME_DELIMITER) == -1) {
            return AppName.ALFRESCO
        }
        return sourceId.substringBefore(EntityRef.APP_NAME_DELIMITER)
    }

    private fun isAlfrescoNodeSourceIdWithEmptyPredicate(sourceId: String, predicate: Predicate?): Boolean {
        return sourceId == AppName.ALFRESCO + EntityRef.APP_NAME_DELIMITER &&
            (predicate == null || predicate == VoidPredicate.INSTANCE)
    }

    class JournalsTotalCountQuery(
        val journals: List<EntityRef>
    )

    class JournalsTotalCountResp(
        val totalCount: List<Long>
    )

    class TotalCountJournalAtts(
        val predicate: Predicate?,
        val sourceId: String?,
        val queryData: ObjectData?
    )

    private class CompletableFutureWrapper<T> : CompletableFuture<T>() {

        lateinit var future: Future<*>

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            val result = future.cancel(mayInterruptIfRunning)
            super.cancel(mayInterruptIfRunning)
            return result
        }
    }
}
