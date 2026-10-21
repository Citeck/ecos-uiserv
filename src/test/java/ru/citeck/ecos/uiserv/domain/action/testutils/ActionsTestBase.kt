package ru.citeck.ecos.uiserv.domain.action.testutils

import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import ru.citeck.ecos.model.lib.ModelServiceFactory
import ru.citeck.ecos.model.lib.type.repo.DefaultTypesRepo
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.model.lib.workspace.api.WorkspaceApi
import ru.citeck.ecos.model.lib.workspace.api.WsMembershipType
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.uiserv.app.common.perms.UiServSystemArtifactPerms
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
    protected lateinit var perms: UiServSystemArtifactPerms
    protected lateinit var workspaceService: WorkspaceService

    /**
     * Workspaces of every user in tests. IDs map to themselves unless overridden below.
     */
    protected val userWorkspaces: MutableSet<String> = mutableSetOf()

    /**
     * Workspaces where every user in tests is a manager.
     */
    protected val managedWorkspaces: MutableSet<String> = mutableSetOf()

    protected val workspaceSystemIds: MutableMap<String, String> = mutableMapOf()

    @BeforeEach
    fun before() {

        val webAppCtxMock = EcosWebAppApiMock("uiserv")

        val recordsServices = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi? {
                return webAppCtxMock
            }
        }

        val modelServices = ModelServiceFactory()
        modelServices.setWorkspaceApi(object : WorkspaceApi {
            override fun getNestedWorkspaces(workspaces: Collection<String>): List<Set<String>> {
                return workspaces.map { emptySet() }
            }
            override fun getUserWorkspaces(user: String, membershipType: WsMembershipType): Set<String> {
                return userWorkspaces
            }
            override fun isUserManagerOf(user: String, workspace: String): Boolean {
                return managedWorkspaces.contains(workspace)
            }
            override fun mapIdentifiers(
                identifiers: List<String>,
                mappingType: WorkspaceApi.IdMappingType
            ): List<String> {
                return identifiers.map { id ->
                    when (mappingType) {
                        WorkspaceApi.IdMappingType.WS_ID_TO_SYS_ID -> workspaceSystemIds[id] ?: id
                        WorkspaceApi.IdMappingType.WS_SYS_ID_TO_ID ->
                            workspaceSystemIds.entries.firstOrNull { it.value == id }?.key ?: id
                        WorkspaceApi.IdMappingType.NO_MAPPING -> id
                    }
                }
            }
        })
        workspaceService = modelServices.workspaceService

        actionDao = ActionInMemDao(recordsServices.predicateService)
        mapper = ActionEntityMapper(actionDao, workspaceService)
        perms = Mockito.mock(UiServSystemArtifactPerms::class.java)

        val actionsDaoProvider = DaoActionsProvider(actionDao, mapper)

        val evaluatorService = RecordEvaluatorServiceImpl()
        evaluatorService.setRecordsServiceFactory(recordsServices)

        records = recordsServices.recordsService
        actionService = ActionService(
            evaluatorService,
            mapper,
            actionDao,
            perms,
            workspaceService
        )
        actionService.setActionProviders(listOf(actionsDaoProvider))

        recordsServices.recordsService.register(
            ActionRecords(
                actionService,
                DefaultTypesRepo(),
                perms,
                workspaceService
            )
        )
    }
}
