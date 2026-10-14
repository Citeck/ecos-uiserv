package ru.citeck.ecos.uiserv.domain.dashboard.api.records

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.uiserv.domain.dashdoard.api.records.DashboardRecords
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService
import ru.citeck.ecos.uiserv.test.workspace.WorkspaceTestSupport

class DashboardRecordsWorkspaceTest {

    private lateinit var recordsService: RecordsService
    private lateinit var service: DashboardService

    @BeforeEach
    fun setUp() {
        val services = RecordsServiceFactory()
        recordsService = services.recordsService
        val modelServices = WorkspaceTestSupport.modelServices()
        service = Mockito.mock(DashboardService::class.java)
        recordsService.register(DashboardRecords(service, Mockito.mock(), Mockito.mock(), modelServices.workspaceService))
    }

    @Test
    fun queryByWorkspaceRefIsMappedAndSkipsScopingForAdmin() {
        query(Predicates.eq("workspaceRef", "emodel/workspace@other-ws"), listOf(AuthRole.ADMIN))

        val findAll = WorkspaceTestSupport.lastInvocation(service, "findAllWithMeta", 6)
        val predStr = findAll.getArgument<Any>(0).toString()
        assertThat(predStr).contains("\"att\":\"workspace\"")
        assertThat(predStr).contains("other-ws")
        assertThat(predStr).doesNotContain("workspaceRef")
        assertThat(predStr).doesNotContain("emodel/workspace@")
        assertThat(findAll.getArgument<Boolean>(5)).isTrue()

        val getCount = WorkspaceTestSupport.lastInvocation(service, "getCount", 3)
        assertThat(getCount.getArgument<Boolean>(2)).isTrue()
        assertThat(getCount.getArgument<Any>(0).toString()).doesNotContain("workspaceRef")
    }

    @Test
    fun queryByGlobalWorkspaceRefIsMappedToEmptyWorkspaceId() {
        query(Predicates.eq("workspaceRef", "emodel/workspace@default"), listOf(AuthRole.ADMIN))

        val predStr = WorkspaceTestSupport.lastInvocation(service, "findAllWithMeta", 6)
            .getArgument<Any>(0).toString()
        assertThat(predStr).contains("\"att\":\"workspace\"")
        assertThat(predStr).doesNotContain("default")
    }

    @Test
    fun queryByAdminWorkspaceRefIsKeptAsIs() {
        query(Predicates.eq("workspaceRef", "emodel/workspace@admin\$someone"), listOf(AuthRole.ADMIN))

        val predStr = WorkspaceTestSupport.lastInvocation(service, "findAllWithMeta", 6)
            .getArgument<Any>(0).toString()
        assertThat(predStr).contains("admin\$someone")
    }

    @Test
    fun queryByWorkspaceRefDoesNotSkipScopingForNonAdmin() {
        query(Predicates.eq("workspaceRef", "emodel/workspace@other-ws"), listOf(AuthRole.USER))

        val findAll = WorkspaceTestSupport.lastInvocation(service, "findAllWithMeta", 6)
        assertThat(findAll.getArgument<Any>(0).toString()).doesNotContain("workspaceRef")
        assertThat(findAll.getArgument<Boolean>(5)).isFalse()
    }

    @Test
    fun journalSearchPredicateDoesNotSkipScoping() {
        query(
            Predicates.or(
                Predicates.contains("moduleId", "text"),
                Predicates.contains("workspaceRef", "text")
            ),
            listOf(AuthRole.ADMIN)
        )

        assertThat(
            WorkspaceTestSupport.lastInvocation(service, "findAllWithMeta", 6).getArgument<Boolean>(5)
        ).isFalse()
    }

    @Test
    fun queryWithoutWorkspaceRefDoesNotSkipScoping() {
        query(Predicates.eq("title", "some-value"), listOf(AuthRole.ADMIN))

        assertThat(
            WorkspaceTestSupport.lastInvocation(service, "findAllWithMeta", 6)
                .getArgument<Boolean>(5)
        ).isFalse()
    }

    private fun query(predicate: Predicate, authorities: List<String>) {
        WorkspaceTestSupport.predicateQuery(recordsService, DashboardRecords.ID, predicate, authorities = authorities)
    }
}
