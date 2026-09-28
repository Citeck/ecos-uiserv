package ru.citeck.ecos.uiserv.domain.action.api.records

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.impl.UserEventActionRecords
import ru.citeck.ecos.uiserv.domain.action.testutils.ActionsTestBase
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ActionWorkspaceReviewTest : ActionsTestBase() {

    private val secretRef = EntityRef.valueOf("uiserv/action@private-ws:secret")

    private fun setupPrivateAction() {
        val action = ActionDto()
        action.id = "secret"
        action.workspace = "private-ws"
        action.name = MLText("Private action")
        action.type = "script"
        action.config = ObjectData.create().set("fn", "private-workspace-script")
        AuthContext.runAsSystem { actionService.updateAction(action) }
        userWorkspaces.add("own-ws")
        managedWorkspaces.add("own-ws")
        assertThat(AuthContext.runAs("outsider") { records.getAtt(secretRef, "name.en").asText() }).isEmpty()
    }

    @Test
    fun `record-actions query must not disclose inaccessible workspace action`() {
        setupPrivateAction()
        records.register(RecordActionsRecords(actionService))
        val result = AuthContext.runAs("outsider") {
            records.queryOne(
                RecordsQuery.create().withSourceId("record-actions")
                    .withQuery(
                        ObjectData.create()
                            .set("records", listOf("test/rec@1"))
                            .set("actions", listOf(secretRef))
                    )
                    .build(),
                listOf("actions[]?json")
            )!!
        }
        assertThat(result["actions[]?json"].asList(ObjectData::class.java)).isEmpty()
    }

    @Test
    fun `action query by records must not disclose inaccessible workspace action`() {
        setupPrivateAction()
        val result = AuthContext.runAs("outsider") {
            records.queryOne(
                RecordsQuery.create().withSourceId("action")
                    .withQuery(
                        ObjectData.create()
                            .set("records", listOf("test/rec@1"))
                            .set("actions", listOf(secretRef))
                    )
                    .build(),
                listOf("actions[]?json")
            )!!
        }
        assertThat(result["actions[]?json"].asList(ObjectData::class.java)).isEmpty()
    }

    @Test
    fun `copy must not disclose inaccessible source workspace action`() {
        setupPrivateAction()
        assertThatThrownBy {
            AuthContext.runAs("outsider") {
                records.mutate(
                    secretRef,
                    ObjectData.create()
                        .set("moduleId", "stolen-copy")
                        .set(RecordConstants.ATT_WORKSPACE, "own-ws")
                )
            }
        }.isInstanceOf(Exception::class.java)
        assertThat(actionService.getAction("own-ws:stolen-copy")).isNull()
    }

    @Test
    fun `outsider must not fire an event of inaccessible workspace action`() {
        setupPrivateAction()
        val action = AuthContext.runAsSystem { actionService.getAction("private-ws:secret") }!!
        action.type = "user-event"
        AuthContext.runAsSystem { actionService.updateAction(action) }
        val events = Mockito.mock(EventsService::class.java, Mockito.RETURNS_DEEP_STUBS)
        records.register(UserEventActionRecords(events, actionService))
        assertThatThrownBy {
            AuthContext.runAs("outsider") {
                records.mutate(
                    EntityRef.valueOf("uiserv/user-event@private-ws:secret"),
                    ObjectData.create().set("eventData", ObjectData.create().set("value", "outsider"))
                )
            }
        }.isInstanceOf(Exception::class.java)
        Mockito.verifyNoInteractions(events)
    }
}
