package ru.citeck.ecos.uiserv.test.workspace

import org.mockito.Mockito
import org.mockito.invocation.Invocation
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.ModelServiceFactory
import ru.citeck.ecos.model.lib.workspace.api.WorkspaceApi
import ru.citeck.ecos.model.lib.workspace.api.WsMembershipType
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

object WorkspaceTestSupport {

    const val USER_WORKSPACE = "test-ws"

    class CapturedQuery(val query: RecordsQuery, val runAsSystem: Boolean)

    private fun workspaceApi(userWorkspaces: Set<String>): WorkspaceApi {
        return object : WorkspaceApi {
            override fun getNestedWorkspaces(workspaces: Collection<String>): List<Set<String>> {
                return workspaces.map { emptySet() }
            }

            override fun getUserWorkspaces(user: String, membershipType: WsMembershipType): Set<String> {
                return userWorkspaces
            }

            override fun isUserManagerOf(user: String, workspace: String): Boolean {
                return true
            }

            override fun mapIdentifiers(
                identifiers: List<String>,
                mappingType: WorkspaceApi.IdMappingType
            ): List<String> {
                return identifiers
            }
        }
    }

    fun modelServices(userWorkspaces: Set<String> = setOf(USER_WORKSPACE)): ModelServiceFactory {
        val modelServices = ModelServiceFactory()
        modelServices.setWorkspaceApi(workspaceApi(userWorkspaces))
        return modelServices
    }

    fun predicateQuery(
        recordsService: RecordsService,
        sourceId: String,
        predicate: Predicate,
        workspaces: List<String> = emptyList(),
        authorities: List<String> = listOf(AuthRole.ADMIN),
        user: String = if (authorities.contains(AuthRole.ADMIN)) "admin" else "user"
    ) {
        val query = RecordsQuery.create()
            .withSourceId(sourceId)
            .withQuery(predicate)
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .withWorkspaces(workspaces)
            .build()
        AuthContext.runAs(user, authorities) {
            recordsService.query(query)
        }
    }

    fun lastInvocation(mock: Any, method: String, argsCount: Int): Invocation {
        return Mockito.mockingDetails(mock).invocations
            .filter { it.method.name == method && it.arguments.size == argsCount }
            .lastOrNull()
            ?: throw AssertionError("no $method invocation with $argsCount args")
    }

    fun registerCapturingSource(
        recordsService: RecordsService,
        sourceId: String
    ): MutableList<CapturedQuery> {
        val captured = mutableListOf<CapturedQuery>()
        recordsService.register(
            object : AbstractRecordsDao(), RecordsQueryDao {
                override fun getId(): String = sourceId
                override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<Any> {
                    captured.add(CapturedQuery(recsQuery, AuthContext.isRunAsSystem()))
                    return RecsQueryRes()
                }
            }
        )
        return captured
    }
}
