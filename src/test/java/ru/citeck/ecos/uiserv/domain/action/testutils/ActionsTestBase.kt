package ru.citeck.ecos.uiserv.domain.action.testutils

import org.junit.jupiter.api.BeforeEach
import ru.citeck.ecos.model.lib.type.repo.DefaultTypesRepo
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.uiserv.domain.action.api.records.ActionRecords
import ru.citeck.ecos.uiserv.domain.action.dao.ActionDao
import ru.citeck.ecos.uiserv.domain.action.service.ActionEntityMapper
import ru.citeck.ecos.uiserv.domain.action.service.ActionService
import ru.citeck.ecos.uiserv.domain.action.service.DaoActionsProvider
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorServiceImpl
import ru.citeck.ecos.webapp.api.EcosWebAppApi

open class ActionsTestBase {

    protected lateinit var actionService: ActionService
    protected lateinit var records: RecordsService
    protected lateinit var mapper: ActionEntityMapper
    protected lateinit var actionDao: ActionDao

    @BeforeEach
    fun before() {

        val webAppCtxMock = EcosWebAppApiMock("uiserv")

        val recordsServices = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi? {
                return webAppCtxMock
            }
        }

        actionDao = ActionInMemDao(recordsServices.predicateService)
        mapper = ActionEntityMapper(actionDao)

        val actionsDaoProvider = DaoActionsProvider(actionDao, mapper)

        val evaluatorService = RecordEvaluatorServiceImpl()
        evaluatorService.setRecordsServiceFactory(recordsServices)

        records = recordsServices.recordsService
        actionService = ActionService(
            evaluatorService,
            mapper,
            actionDao
        )
        actionService.setActionProviders(listOf(actionsDaoProvider))

        recordsServices.recordsService.register(
            ActionRecords(
                actionService,
                DefaultTypesRepo()
            )
        )
    }
}
