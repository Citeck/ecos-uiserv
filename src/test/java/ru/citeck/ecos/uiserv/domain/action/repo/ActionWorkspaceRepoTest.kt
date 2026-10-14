package ru.citeck.ecos.uiserv.domain.action.repo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.type.repo.DefaultTypesRepo
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.app.common.perms.UiServSystemArtifactPerms
import ru.citeck.ecos.uiserv.domain.action.api.records.ActionRecords
import ru.citeck.ecos.uiserv.domain.action.dao.ActionDao
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.service.ActionEntityMapper
import ru.citeck.ecos.uiserv.domain.action.service.ActionService
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorService
import ru.citeck.ecos.uiserv.test.workspace.WorkspaceTestSupport
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class ActionWorkspaceRepoTest {

    companion object {
        private const val ID = "ws-repo-test-action"
    }

    @Autowired
    lateinit var actionService: ActionService

    @Autowired
    lateinit var actionDao: ActionDao

    @Autowired
    lateinit var actionRepository: ActionRepository

    @AfterEach
    fun cleanup() {
        actionRepository.deleteAll(actionRepository.findAll().filter { it.extId.startsWith(ID) })
    }

    private fun save(workspace: String) {
        val action = ActionDto()
        action.id = ID
        action.workspace = workspace
        action.name = MLText("name-$workspace")
        AuthContext.runAsSystem { actionService.updateAction(action) }
    }

    @Test
    fun workspaceRefFilterUsesSqlForRowsAndCount() {
        save("")
        save("ws-a")
        save("ws-b")
        val workspaceService = WorkspaceTestSupport.modelServices(setOf("ws-a")).workspaceService
        val perms = Mockito.mock(UiServSystemArtifactPerms::class.java)
        val queryService = ActionService(
            Mockito.mock(RecordEvaluatorService::class.java),
            ActionEntityMapper(actionDao, workspaceService),
            actionDao,
            perms,
            workspaceService
        )
        val records = RecordsServiceFactory().recordsService
        records.register(ActionRecords(queryService, DefaultTypesRepo(), perms, workspaceService))
        val query = RecordsQuery.create()
            .withSourceId(ActionRecords.ID)
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .withWorkspaces(listOf("admin\$admin"))
            .withQuery(
                Predicates.and(
                    Predicates.eq("moduleId", ID),
                    ValuePredicate("workspaceRef", ValuePredicate.Type.CONTAINS, listOf("emodel/workspace@default", "emodel/workspace@ws-b"))
                )
            )
            .withMaxItems(1)
            .build()
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val first = records.query(query)
            val second = records.query(query.copy().withSkipCount(1).build())
            assertThat(first.getTotalCount()).isEqualTo(2)
            assertThat(second.getTotalCount()).isEqualTo(2)
            assertThat(first.getRecords()).hasSize(1)
            assertThat(second.getRecords()).hasSize(1)
            assertThat((first.getRecords() + second.getRecords()).map { it.getLocalId() })
                .containsExactlyInAnyOrder(ID, "ws-b:$ID")
        }
        AuthContext.runAs("user", listOf(AuthRole.USER)) {
            assertThat(records.query(query).getRecords().map { it.getLocalId() }).containsExactly(ID)
            assertThat(records.query(query).getTotalCount()).isEqualTo(1)
        }
    }

    @Test
    fun `the same id is stored once per workspace`() {

        save("")
        save("ws-a")
        save("ws-b")
        // update, not insert
        save("ws-a")
        save("default")

        val stored = actionRepository.findAll().filter { it.extId == ID }
        assertThat(stored.map { it.workspace }).containsExactlyInAnyOrder("", "ws-a", "ws-b")

        assertThat(AuthContext.runAsSystem { actionService.getAction(IdInWs.create("ws-a", ID)) }!!.name.getClosest()).isEqualTo("name-ws-a")
        assertThat(actionService.getAction(IdInWs.create(ID))!!.name.getClosest()).isEqualTo("name-default")

        val globalAndWsA = actionDao.getActions(
            Predicates.and(Predicates.eq("extId", ID), Predicates.inVals("workspace", listOf("", "ws-a"))),
            100,
            0,
            emptyList()
        )
        assertThat(globalAndWsA.map { it.workspace }).containsExactlyInAnyOrder("", "ws-a")

        AuthContext.runAsSystem { actionService.deleteAction(IdInWs.create("ws-b", ID)) }
        assertThat(actionRepository.findAll().filter { it.extId == ID }.map { it.workspace })
            .containsExactlyInAnyOrder("", "ws-a")
    }
}
