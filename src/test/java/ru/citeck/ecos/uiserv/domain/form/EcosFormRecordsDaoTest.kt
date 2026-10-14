package ru.citeck.ecos.uiserv.domain.form

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.form.api.records.EcosFormRecordsDao
import ru.citeck.ecos.uiserv.domain.form.api.records.EcosResolvedFormRecordsDao
import ru.citeck.ecos.uiserv.domain.form.registry.FormsRegistryConfiguration
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormServiceImpl
import ru.citeck.ecos.uiserv.domain.workspace.service.WorkspaceUiService
import ru.citeck.ecos.uiserv.test.workspace.WorkspaceTestSupport
import ru.citeck.ecos.uiserv.test.workspace.WorkspaceTestSupport.CapturedQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef

class EcosFormRecordsDaoTest {

    lateinit var recordsService: RecordsService
    lateinit var ecosFormService: EcosFormService

    @BeforeEach
    fun setUp() {
        val services = RecordsServiceFactory()
        recordsService = services.recordsService
        val modelServices = WorkspaceTestSupport.modelServices()
        ecosFormService = EcosFormServiceImpl(
            FormsEntityInMemDao(services.predicateService, modelServices.workspaceService),
            services.recordsService,
            modelServices.workspaceService
        )
        val workspaceUiService = WorkspaceUiService(
            Mockito.mock(),
            Mockito.mock(),
            modelServices.workspaceService
        )
        val formsRecordsDao = EcosFormRecordsDao(
            ecosFormService,
            null,
            modelServices.workspaceService,
            workspaceUiService
        )
        recordsService.register(formsRecordsDao)

        val typeService = Mockito.mock(EcosTypeService::class.java)
        val resolvedFormsRecordsDao = EcosResolvedFormRecordsDao(
            formsRecordsDao,
            typeService,
            ecosFormService,
            modelServices.workspaceService
        )
        recordsService.register(resolvedFormsRecordsDao)
    }

    @Test
    fun workspaceRefReturnsCorrectEntityRef() {
        val ref = AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val mutAtts = RecordAtts(EntityRef.create("", "form", ""))
            mutAtts.setAtt("id", "form-ws-test")
            mutAtts.setAtt("_workspace", "test-ws")
            recordsService.mutate(mutAtts)
        }

        val wsRef = recordsService.getAtt(ref, "workspaceRef?id").asText()
        assertThat(wsRef).isEqualTo("emodel/workspace@test-ws")
    }

    @Test
    fun workspaceRefReturnsDefaultForGlobalForm() {
        val ref = AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val mutAtts = RecordAtts(EntityRef.create("", "form", ""))
            mutAtts.setAtt("id", "form-global-test")
            recordsService.mutate(mutAtts)
        }

        val wsRef = recordsService.getAtt(ref, "workspaceRef?id").asText()
        assertThat(wsRef).isEqualTo("emodel/workspace@default")
    }

    @Test
    fun queryByWorkspaceRefPredicateMapsToWorkspace() {
        val captured = capturingRegistrySource()

        query(Predicates.eq("workspaceRef", "emodel/workspace@other-ws"))

        val predStr = captured.last().query.getQuery(Predicate::class.java).toString()
        assertThat(predStr).contains("\"att\":\"workspace\"")
        assertThat(predStr).contains("other-ws")
        assertThat(predStr).doesNotContain("workspaceRef")
        assertThat(predStr).doesNotContain("emodel/workspace@")
    }

    @Test
    fun formOfNonMemberWorkspaceIsReadableByAdminOnly() {
        val ref = AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val mutAtts = RecordAtts(EntityRef.create("", "form", ""))
            mutAtts.setAtt("id", "form-in-foreign-ws")
            mutAtts.setAtt("_workspace", "foreign-ws")
            recordsService.mutate(mutAtts)
        }

        val asAdmin = AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            recordsService.getAtts(ref, listOf("moduleId", "workspaceRef?id"))
        }
        assertThat(asAdmin.getAtt("moduleId").asText()).isEqualTo("form-in-foreign-ws")
        assertThat(asAdmin.getAtt("workspaceRef?id").asText()).isEqualTo("emodel/workspace@foreign-ws")

        val asUser = AuthContext.runAs("user", listOf(AuthRole.USER)) {
            recordsService.getAtt(ref, "moduleId").asText()
        }
        assertThat(asUser).isBlank()
    }

    @Test
    fun queryByAdminWorkspaceRefIsNormalizedToEmptyWorkspaceId() {
        val captured = capturingRegistrySource()

        query(Predicates.eq("workspaceRef", "emodel/workspace@admin\$someone"))

        val predStr = captured.last().query.getQuery(Predicate::class.java).toString()
        assertThat(predStr).doesNotContain("admin")
    }

    @Test
    fun queryByGlobalWorkspaceRefIsMappedToEmptyWorkspaceId() {
        val captured = capturingRegistrySource()

        query(Predicates.eq("workspaceRef", "emodel/workspace@default"))

        val predStr = captured.last().query.getQuery(Predicate::class.java).toString()
        assertThat(predStr).contains("\"att\":\"workspace\"")
        assertThat(predStr).doesNotContain("default")
    }

    @Test
    fun queryByEmptyWorkspaceRefPredicateMapsToEmptyWorkspace() {
        val captured = capturingRegistrySource()

        query(Predicates.empty("workspaceRef"))

        val predStr = captured.last().query.getQuery(Predicate::class.java).toString()
        assertThat(predStr).contains("\"att\":\"workspace\"")
        assertThat(predStr).doesNotContain("workspaceRef")
    }

    @Test
    fun queryByWorkspaceRefClearsQueriedWorkspacesAndRunsAsSystem() {
        val captured = capturingRegistrySource()

        query(Predicates.eq("workspaceRef", "emodel/workspace@other-ws"), workspaces = listOf("test-ws"))

        assertThat(captured.last().query.workspaces).isEmpty()
        assertThat(captured.last().runAsSystem).isTrue()
    }

    @Test
    fun journalSearchPredicateKeepsWorkspaceScoping() {
        val captured = capturingRegistrySource()

        query(
            Predicates.or(
                Predicates.contains("moduleId", "text"),
                Predicates.contains("workspaceRef", "text")
            ),
            workspaces = listOf("test-ws")
        )

        assertThat(captured.last().query.workspaces).containsExactly("test-ws")
        assertThat(captured.last().runAsSystem).isFalse()
    }

    @Test
    fun queryWithoutWorkspaceRefDoesNotClearWorkspaces() {
        val captured = capturingRegistrySource()

        query(Predicates.eq("moduleId", "some-form"), workspaces = listOf("test-ws"))

        val predStr = captured.last().query.getQuery(Predicate::class.java).toString()
        assertThat(predStr).contains("\"att\":\"id\"")
        assertThat(predStr).doesNotContain("workspaceRef")
        assertThat(captured.last().query.workspaces).containsExactly("test-ws")
        assertThat(captured.last().runAsSystem).isFalse()
    }

    @Test
    fun queryByWorkspaceRefWithoutAdminAuthKeepsScoping() {
        val captured = capturingRegistrySource()

        query(
            Predicates.eq("workspaceRef", "emodel/workspace@other-ws"),
            workspaces = listOf("test-ws"),
            authorities = listOf(AuthRole.USER)
        )

        assertThat(captured.last().query.workspaces).containsExactly("test-ws")
        assertThat(captured.last().runAsSystem).isFalse()
    }

    private fun query(
        predicate: Predicate,
        workspaces: List<String> = emptyList(),
        authorities: List<String> = listOf(AuthRole.ADMIN)
    ) {
        WorkspaceTestSupport.predicateQuery(recordsService, "form", predicate, workspaces, authorities)
    }

    private fun capturingRegistrySource(): MutableList<CapturedQuery> {
        return WorkspaceTestSupport.registerCapturingSource(recordsService, FormsRegistryConfiguration.FORMS_REGISTRY_SOURCE_ID)
    }
}
