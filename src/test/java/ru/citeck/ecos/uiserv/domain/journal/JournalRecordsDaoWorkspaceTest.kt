package ru.citeck.ecos.uiserv.domain.journal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.uiserv.domain.journal.api.records.JournalRecordsDao
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta
import ru.citeck.ecos.uiserv.domain.journal.registry.JournalsRegistryConfiguration
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.uiserv.domain.workspace.service.WorkspaceUiService
import ru.citeck.ecos.uiserv.test.workspace.WorkspaceTestSupport
import ru.citeck.ecos.uiserv.test.workspace.WorkspaceTestSupport.CapturedQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef

class JournalRecordsDaoWorkspaceTest {

    lateinit var recordsService: RecordsService
    lateinit var workspaceService: WorkspaceService
    lateinit var journalService: JournalService

    @BeforeEach
    fun setUp() {
        val services = RecordsServiceFactory()
        recordsService = services.recordsService
        val modelServices = WorkspaceTestSupport.modelServices()
        workspaceService = modelServices.workspaceService

        journalService = Mockito.mock(JournalService::class.java)
        val workspaceUiService = WorkspaceUiService(
            Mockito.mock(),
            Mockito.mock(),
            workspaceService
        )

        val journalRecordsDao = JournalRecordsDao(
            journalService,
            Mockito.mock(),
            Mockito.mock(),
            workspaceService,
            workspaceUiService
        )
        recordsService.register(journalRecordsDao)
    }

    @Test
    fun workspaceRefReturnsCorrectEntityRef() {
        val record = JournalRecordsDao.JournalRecord(
            JournalWithMeta(
                JournalDef.create().withId("test-journal").withWorkspace("test-ws").build()
            ),
            workspaceService
        )
        assertThat(record.getWorkspaceRef()).isEqualTo(
            EntityRef.create("emodel", "workspace", "test-ws")
        )
    }

    @Test
    fun workspaceRefReturnsDefaultForGlobalJournal() {
        val record = JournalRecordsDao.JournalRecord(
            JournalWithMeta(
                JournalDef.create().withId("global-journal").withWorkspace("").build()
            ),
            workspaceService
        )
        assertThat(record.getWorkspaceRef()).isEqualTo(
            EntityRef.create("emodel", "workspace", "default")
        )
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
    fun journalOfNonMemberWorkspaceIsReadableByAdminOnly() {
        val localId = workspaceService.addWsPrefixToId("journal-in-foreign-ws", "foreign-ws")
        Mockito.`when`(journalService.getJournalById(workspaceService.convertToIdInWs(localId))).thenReturn(
            JournalWithMeta(
                JournalDef.create().withId("journal-in-foreign-ws").withWorkspace("foreign-ws").build()
            )
        )
        val ref = EntityRef.create("", "journal", localId)

        val asAdmin = AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            recordsService.getAtts(ref, listOf("moduleId", "workspaceRef?id"))
        }
        assertThat(asAdmin.getAtt("moduleId").asText()).isEqualTo("journal-in-foreign-ws")
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

        query(Predicates.eq("name", "some-journal"), workspaces = listOf("test-ws"))

        val predStr = captured.last().query.getQuery(Predicate::class.java).toString()
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
        WorkspaceTestSupport.predicateQuery(recordsService, "journal", predicate, workspaces, authorities)
    }

    private fun capturingRegistrySource(): MutableList<CapturedQuery> {
        return WorkspaceTestSupport.registerCapturingSource(recordsService, JournalsRegistryConfiguration.JOURNALS_REGISTRY_SOURCE_ID)
    }
}
