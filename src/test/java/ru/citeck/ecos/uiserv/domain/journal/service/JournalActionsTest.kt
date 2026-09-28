package ru.citeck.ecos.uiserv.domain.journal.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.action.dto.ExecForQueryConfig
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalActionDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.context.WorkspaceApiMock
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class JournalActionsTest {

    @Autowired
    lateinit var journalsService: JournalService

    @Autowired
    lateinit var recordsService: RecordsService

    @Autowired
    lateinit var workspaceService: WorkspaceService

    @Autowired
    lateinit var workspaceApiMock: WorkspaceApiMock

    @Test
    fun test() {

        journalsService.save(
            JournalDef.create()
                .withId("some-journal")
                .withActionsDef(
                    listOf(
                        JournalActionDef.create()
                            .withId("some-action")
                            .withType("some-type")
                            .withExecForQueryConfig(ExecForQueryConfig(true))
                            .build()
                    )
                )
                .build()
        )

        val actionRefs = recordsService.getAtt(
            EntityRef.create("rjournal", "some-journal"),
            "actionsDef[].id"
        ).asStrList()

        assertThat(actionRefs).containsExactly("journal\$some-journal\$some-action")
        val actionRef = EntityRef.create("action", actionRefs[0])
        val execForQueryConfig = recordsService.getAtt(actionRef, "execForQueryConfig?json")

        assertTrue(execForQueryConfig.get("execAsForRecords").asBoolean())
    }

    @Test
    fun `inline action of a journal in workspace is resolved from this journal`() {

        val journalDef = JournalDef.create()
            .withId("ws-journal")
            .withActionsDef(
                listOf(
                    JournalActionDef.create()
                        .withId("ws-action")
                        .withType("ws-type")
                        .build()
                )
            )
        journalsService.save(journalDef.build())
        journalsService.save(journalDef.withWorkspace("ws-a").build())

        workspaceApiMock.addMember("ws-a", "ws-user")
        val wsJournalId = workspaceService.addWsPrefixToId("ws-journal", "ws-a")
        assertThat(wsJournalId).isNotEqualTo("ws-journal")

        val wsActionIds = AuthContext.runAs("ws-user") {
            recordsService.getAtt(EntityRef.create("rjournal", wsJournalId), "actionsDef[].id").asStrList()
        }
        assertThat(wsActionIds).containsExactly("journal$${wsJournalId}\$ws-action")

        val wsType = AuthContext.runAs("ws-user") {
            recordsService.getAtt(EntityRef.create("action", wsActionIds[0]), "type").asText()
        }
        assertThat(wsType).isEqualTo("ws-type")
    }
}
