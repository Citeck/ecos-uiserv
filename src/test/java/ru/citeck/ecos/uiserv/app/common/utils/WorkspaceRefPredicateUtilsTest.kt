package ru.citeck.ecos.uiserv.app.common.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.AndPredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.uiserv.test.workspace.WorkspaceTestSupport

class WorkspaceRefPredicateUtilsTest {

    private lateinit var workspaceService: WorkspaceService

    @BeforeEach
    fun setUp() {
        workspaceService = WorkspaceTestSupport.modelServices().workspaceService
    }

    private fun map(predicate: Predicate?, attsMapping: Map<String, String> = emptyMap()): Predicate {
        return WorkspaceRefPredicateUtils.mapWorkspaceRefPredicate(
            predicate,
            WorkspaceRefPredicateUtils.globalWsRuleOf(workspaceService),
            attsMapping
        )
    }

    private fun mapBlankOrDefault(predicate: Predicate?): Predicate {
        return WorkspaceRefPredicateUtils.mapWorkspaceRefPredicate(
            predicate,
            WorkspaceRefPredicateUtils.blankOrDefaultWsRule()
        )
    }

    @Test
    fun associationPickerRefsBecomeExactWorkspaceMembership() {
        val result = map(
            ValuePredicate("workspaceRef", ValuePredicate.Type.CONTAINS, listOf("emodel/workspace@default", "emodel/workspace@ws-a"))
        ) as ValuePredicate
        assertThat(result.getType()).isEqualTo(ValuePredicate.Type.IN)
        assertThat(result.getAttribute()).isEqualTo("workspace")
        assertThat(result.getValue().toStrList()).containsExactly("", "ws-a")
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            assertThat(WorkspaceRefPredicateUtils.isWorkspaceScopingSkipAllowed(result)).isTrue()
        }
        AuthContext.runAs("user", listOf(AuthRole.USER)) {
            assertThat(WorkspaceRefPredicateUtils.isWorkspaceScopingSkipAllowed(result)).isFalse()
        }
    }

    @Test
    fun scalarWorkspaceRefSelectionBecomesEquality() {
        val result = map(Predicates.contains("workspaceRef", "emodel/workspace@ws-a")) as ValuePredicate
        assertThat(result.getType()).isEqualTo(ValuePredicate.Type.EQ)
        assertThat(result.getValue().asText()).isEqualTo("ws-a")
    }

    @Test
    fun freeTextAndOtherAssociationsAreNotWorkspaceSelections() {
        for (value in listOf("ws-a", "emodel/person@user", "emodel/workspace@")) {
            val result = map(ValuePredicate("workspaceRef", ValuePredicate.Type.CONTAINS, listOf(value))) as ValuePredicate
            assertThat(result.getType()).isEqualTo(ValuePredicate.Type.CONTAINS)
            assertThat(result.getValue().toStrList()).containsExactly(value)
            AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
                assertThat(WorkspaceRefPredicateUtils.isWorkspaceScopingSkipAllowed(result)).isFalse()
            }
        }
    }

    @Test
    fun eqByWorkspaceRefIsMappedToWorkspaceLocalId() {
        val res = map(Predicates.eq("workspaceRef", "emodel/workspace@some-ws")) as ValuePredicate

        assertThat(res.getAttribute()).isEqualTo("workspace")
        assertThat(res.getValue().asText()).isEqualTo("some-ws")
    }

    @Test
    fun inValsByWorkspaceRefIsMappedToWorkspaceLocalIds() {
        val res = map(
            Predicates.inVals("workspaceRef", listOf("emodel/workspace@ws-a", "emodel/workspace@ws-b"))
        ) as ValuePredicate

        assertThat(res.getAttribute()).isEqualTo("workspace")
        assertThat(res.getValue().toStrList()).containsExactly("ws-a", "ws-b")
    }

    @Test
    fun globalWorkspaceRefIsNormalizedToEmptyId() {
        val byDefault = map(Predicates.eq("workspaceRef", "emodel/workspace@default")) as ValuePredicate
        assertThat(byDefault.getValue().asText()).isEmpty()

        val byAdminWs = map(Predicates.eq("workspaceRef", "emodel/workspace@admin\$someone")) as ValuePredicate
        assertThat(byAdminWs.getValue().asText()).isEmpty()
    }

    @Test
    fun blankOrDefaultRuleKeepsAdminWorkspaceAsIs() {
        val byDefault = mapBlankOrDefault(
            Predicates.eq("workspaceRef", "emodel/workspace@default")
        ) as ValuePredicate
        assertThat(byDefault.getValue().asText()).isEmpty()

        val byAdminWs = mapBlankOrDefault(
            Predicates.eq("workspaceRef", "emodel/workspace@admin\$someone")
        ) as ValuePredicate
        assertThat(byAdminWs.getValue().asText()).isEqualTo("admin\$someone")
    }

    @Test
    fun globalWorkspaceRefIsNormalizedInsideArrays() {
        val res = map(
            Predicates.inVals("workspaceRef", listOf("emodel/workspace@default", "emodel/workspace@ws-a"))
        ) as ValuePredicate

        assertThat(res.getValue().toStrList()).containsExactly("", "ws-a")
    }

    @Test
    fun emptyWorkspaceRefKeepsPredicateTypeAndRenamesAttribute() {
        val res = map(Predicates.empty("workspaceRef"))

        assertThat(res.toString()).contains("\"att\":\"workspace\"")
        assertThat(res.toString()).doesNotContain("workspaceRef")
    }

    @Test
    fun nestedAndNegatedPredicatesAreMapped() {
        val res = map(
            Predicates.and(
                Predicates.eq("moduleId", "some-id"),
                Predicates.or(
                    Predicates.eq("workspaceRef", "emodel/workspace@ws-a"),
                    Predicates.not(Predicates.eq("workspaceRef", "emodel/workspace@ws-b"))
                )
            )
        )

        val str = res.toString()
        assertThat(str).doesNotContain("workspaceRef")
        assertThat(str).doesNotContain("emodel/workspace@")
        assertThat(str).contains("ws-a")
        assertThat(str).contains("ws-b")
        assertThat(PredicateUtils.getAllPredicateAttributes(res)).contains("workspace")
    }

    @Test
    fun additionalAttsMappingIsApplied() {
        val res = map(Predicates.eq("moduleId", "some-id"), mapOf("moduleId" to "id")) as ValuePredicate

        assertThat(res.getAttribute()).isEqualTo("id")
    }

    @Test
    fun emptyCompositePredicateIsPreservedInsteadOfCollapsingToAlwaysFalse() {
        val res = map(Predicates.and())

        assertThat(res).isInstanceOf(AndPredicate::class.java)
        assertThat(PredicateUtils.isAlwaysFalse(res))
            .`as`("an unfiltered journal query must not be turned into a match-nothing predicate")
            .isFalse()
    }

    @Test
    fun nullPredicateIsMappedToAlwaysFalse() {
        assertThat(PredicateUtils.isAlwaysFalse(map(null))).isTrue()
    }

    @Test
    fun journalSearchPredicateDoesNotGrantScopingSkip() {
        val searchLike = map(
            Predicates.or(
                Predicates.contains("moduleId", "some text"),
                Predicates.contains("workspaceRef", "some text")
            )
        )

        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            assertThat(WorkspaceRefPredicateUtils.isWorkspaceScopingSkipAllowed(searchLike)).isFalse()
        }
    }

    @Test
    fun searchTextIsNotParsedAsEntityRef() {
        val searchLike = map(Predicates.contains("workspaceRef", "some@text")) as ValuePredicate

        assertThat(searchLike.getAttribute()).isEqualTo("workspace")
        assertThat(searchLike.getValue().asText()).isEqualTo("some@text")
    }

    @Test
    fun inValsAndEmptyWorkspaceFiltersGrantScopingSkip() {
        val byIn = map(Predicates.inVals("workspaceRef", listOf("emodel/workspace@ws-a")))
        val byEmpty = map(Predicates.empty("workspaceRef"))

        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            assertThat(WorkspaceRefPredicateUtils.isWorkspaceScopingSkipAllowed(byIn)).isTrue()
            assertThat(WorkspaceRefPredicateUtils.isWorkspaceScopingSkipAllowed(byEmpty)).isTrue()
        }
    }

    @Test
    fun scopingSkipRequiresBothAdminAuthAndWorkspaceFilter() {
        val withWs = map(Predicates.eq("workspaceRef", "emodel/workspace@ws-a"))
        val withoutWs = map(Predicates.eq("moduleId", "x"))

        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            assertThat(WorkspaceRefPredicateUtils.isWorkspaceScopingSkipAllowed(withWs)).isTrue()
            assertThat(WorkspaceRefPredicateUtils.isWorkspaceScopingSkipAllowed(withoutWs)).isFalse()
        }
        AuthContext.runAs("user", listOf(AuthRole.USER)) {
            assertThat(WorkspaceRefPredicateUtils.isWorkspaceScopingSkipAllowed(withWs)).isFalse()
        }
    }
}
