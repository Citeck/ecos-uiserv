package ru.citeck.ecos.uiserv.domain.action.api.records

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.testutils.ActionsTestBase
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ActionWorkspaceFilterTest : ActionsTestBase() {

    companion object {
        private const val MEMBER_WS = "member-ws"
        private const val FOREIGN_WS = "foreign-ws"
    }

    @BeforeEach
    fun createFixtures() {
        userWorkspaces.add(MEMBER_WS)
        AuthContext.runAsSystem {
            for (workspace in listOf("", MEMBER_WS, FOREIGN_WS)) {
                actionService.updateAction(
                    ActionDto().apply {
                        id = "same-action"
                        this.workspace = workspace
                        name = MLText("Action $workspace")
                        type = "view"
                    }
                )
            }
        }
    }

    @Test
    fun adminWorkspaceDefaultsToGlobalActions() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            assertQuery(Predicates.alwaysTrue(), listOf("admin\$admin"), "same-action")
        }
    }

    @Test
    fun adminCanFilterAndOpenForeignWorkspaceAction() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            assertQuery(byWorkspace(FOREIGN_WS), listOf("admin\$admin"), "$FOREIGN_WS:same-action")
            assertThat(records.getAtt(ref("$FOREIGN_WS:same-action"), "workspaceRef?id").asText())
                .isEqualTo("emodel/workspace@$FOREIGN_WS")
        }
    }

    @Test
    fun systemExplicitFilterOverridesRequestedWorkspaceScope() {
        AuthContext.runAsSystem {
            assertQuery(byWorkspace(FOREIGN_WS), listOf("default"), "$FOREIGN_WS:same-action")
        }
    }

    @Test
    fun globalAliasesMatchGlobalActionsAndExposeDefaultWorkspaceRef() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            for (workspace in listOf("default", "admin\$admin")) {
                assertQuery(byWorkspace(workspace), listOf(MEMBER_WS), "same-action")
            }
            assertThat(records.getAtt(ref("same-action"), "workspaceRef?id").asText())
                .isEqualTo("emodel/workspace@default")
        }
    }

    @Test
    fun ordinaryUserCannotFilterOrOpenForeignWorkspaceAction() {
        AuthContext.runAs("user", listOf(AuthRole.USER)) {
            assertQuery(byWorkspace(FOREIGN_WS), listOf(MEMBER_WS))
            assertThat(records.getAtt(ref("$FOREIGN_WS:same-action"), "workspaceRef?id").asText()).isEmpty()
            assertQuery(byWorkspace(MEMBER_WS), listOf(MEMBER_WS), "$MEMBER_WS:same-action")
            assertThat(actionService.getActions(Predicates.alwaysTrue(), listOf(MEMBER_WS), 10, 0, emptyList(), true))
                .extracting<String> { it.workspace }.containsExactly(MEMBER_WS)
            assertThat(actionService.getCount(Predicates.alwaysTrue(), listOf(MEMBER_WS), true)).isEqualTo(1)
        }
    }

    @Test
    fun textSearchDoesNotRemoveDefaultWorkspaceScope() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            assertQuery(
                Predicates.or(Predicates.contains("moduleId", "same"), Predicates.contains("workspaceRef", "ws")),
                listOf("admin\$admin"),
                "same-action"
            )
        }
    }

    @Test
    fun compositeWorkspaceFilterKeepsPaginationAndCountConsistent() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val predicate = Predicates.and(
                Predicates.eq("moduleId", "same-action"),
                Predicates.inVals("workspaceRef", listOf("emodel/workspace@default", "emodel/workspace@$FOREIGN_WS"))
            )
            val page = records.query(query(predicate, listOf("admin\$admin")).withMaxItems(1).build())
            assertThat(page.getRecords()).hasSize(1)
            assertThat(page.getRecords().first().getLocalId()).isIn("same-action", "$FOREIGN_WS:same-action")
            assertThat(page.getTotalCount()).isEqualTo(2)
            assertQuery(
                Predicates.not(byWorkspace("default")),
                listOf("admin\$admin"),
                "$MEMBER_WS:same-action",
                "$FOREIGN_WS:same-action"
            )
        }
    }

    private fun byWorkspace(workspace: String) = ValuePredicate("workspaceRef", ValuePredicate.Type.CONTAINS, listOf("emodel/workspace@$workspace"))

    private fun ref(localId: String) = EntityRef.create("uiserv", ActionRecords.ID, localId)

    private fun query(predicate: Predicate, workspaces: List<String>) = RecordsQuery.create()
        .withSourceId(ActionRecords.ID)
        .withLanguage(PredicateService.LANGUAGE_PREDICATE)
        .withQuery(predicate)
        .withWorkspaces(workspaces)

    private fun assertQuery(predicate: Predicate, workspaces: List<String>, vararg expected: String) {
        val result = records.query(query(predicate, workspaces).build())
        assertThat(result.getRecords().map { it.getLocalId() }).containsExactlyInAnyOrder(*expected)
        assertThat(result.getTotalCount()).isEqualTo(expected.size.toLong())
    }
}
