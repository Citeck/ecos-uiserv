package ru.citeck.ecos.uiserv.domain.board

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository
import ru.citeck.ecos.uiserv.domain.board.service.BoardServiceImpl
import ru.citeck.ecos.uiserv.test.workspace.WorkspaceTestSupport
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class BoardWorkspaceScopingTest {

    companion object {
        private const val BOARD_ID = "coredev90-scoping"
        private const val MEMBER_WS = "coredev90-member"
        private const val FOREIGN_WS = "coredev90-foreign"
        private val WORKSPACES = listOf("", MEMBER_WS, FOREIGN_WS)
    }

    @Autowired
    private lateinit var repository: BoardRepository

    @Autowired
    private lateinit var converterFactory: JpaSearchConverterFactory

    private lateinit var service: BoardServiceImpl

    @BeforeEach
    fun setUp() {
        // Only remote workspace membership is substituted: service, scoping and SQL are real.
        val workspaceService = WorkspaceTestSupport.modelServices(setOf(MEMBER_WS)).workspaceService
        service = BoardServiceImpl(repository, workspaceService, converterFactory)
        service.init()
        AuthContext.runAsSystem {
            for (workspace in WORKSPACES) {
                val board = BoardDef(BOARD_ID)
                board.workspace = workspace
                service.save(board)
            }
        }
    }

    @AfterEach
    fun tearDown() {
        AuthContext.runAsSystem {
            for (workspace in WORKSPACES) {
                service.delete(IdInWs.create(workspace, BOARD_ID))
            }
        }
    }

    @Test
    fun adminCanQueryForeignWorkspaceWhenScopingIsExplicitlySkipped() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            assertQuery(true, listOf("admin\$admin"), Predicates.eq("workspace", FOREIGN_WS), FOREIGN_WS)
        }
    }

    @Test
    fun systemCanQueryForeignWorkspaceWhenScopingIsExplicitlySkipped() {
        AuthContext.runAsSystem {
            assertQuery(true, listOf("admin\$admin"), Predicates.eq("workspace", FOREIGN_WS), FOREIGN_WS)
        }
    }

    @Test
    fun adminKeepsDefaultGlobalScopeWithoutExplicitSkip() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            assertQuery(false, listOf("admin\$admin"), Predicates.alwaysTrue(), "default")
        }
    }

    @Test
    fun nonAdminCannotUseSkipToQueryForeignWorkspace() {
        AuthContext.runAs("user", listOf(AuthRole.USER)) {
            assertQuery(true, listOf(MEMBER_WS), Predicates.eq("workspace", FOREIGN_WS))
        }
    }

    @Test
    fun nonAdminSkipStillRestrictsResultsToGlobalAndMemberWorkspaces() {
        AuthContext.runAs("user", listOf(AuthRole.USER)) {
            assertQuery(true, listOf("default", MEMBER_WS), Predicates.alwaysTrue(), "default", MEMBER_WS)
        }
    }

    @Test
    fun adminBypassPreservesRequestedFilterAndPagination() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val predicate = Predicates.and(
                Predicates.eq("extId", BOARD_ID),
                Predicates.inVals("workspace", listOf(MEMBER_WS, FOREIGN_WS))
            )
            val page = service.getAll(predicate, listOf("admin\$admin"), 1, 0, emptyList(), true)
            assertThat(page).hasSize(1)
            assertThat(page.first().boardDef.workspace).isIn(MEMBER_WS, FOREIGN_WS)
            assertThat(service.getCount(predicate, listOf("admin\$admin"), true)).isEqualTo(2)
        }
    }

    private fun assertQuery(skip: Boolean, queriedWorkspaces: List<String>, filter: Predicate, vararg expected: String) {
        val predicate = Predicates.and(Predicates.eq("extId", BOARD_ID), filter)
        val result = service.getAll(predicate, queriedWorkspaces, 10, 0, emptyList(), skip)
        assertThat(result.map { it.boardDef.workspace }).containsExactlyInAnyOrder(*expected)
        assertThat(service.getCount(predicate, queriedWorkspaces, skip)).isEqualTo(expected.size.toLong())
    }
}
