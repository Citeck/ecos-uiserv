package ru.citeck.ecos.uiserv.domain.action.api.records

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.eapps.ActionArtifactHandler
import ru.citeck.ecos.uiserv.domain.action.testutils.ActionsTestBase
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ActionWorkspaceTest : ActionsTestBase() {

    companion object {
        private const val WS_1 = "ws-one"
        private const val WS_2 = "ws-two"
        private const val USER = "fet"
    }

    private fun createAction(id: String, workspace: String, name: String = "$id-$workspace") {
        val action = ActionDto()
        action.id = id
        action.workspace = workspace
        action.name = MLText(name)
        action.type = "test-type"
        AuthContext.runAsSystem { actionService.updateAction(action) }
    }

    private fun sysGetAction(id: String): ActionDto? = AuthContext.runAsSystem { actionService.getAction(id) }

    private fun sysGetAction(id: IdInWs): ActionDto? = AuthContext.runAsSystem { actionService.getAction(id) }

    private fun actionRef(localId: String) = EntityRef.create("uiserv", ActionRecords.ID, localId)

    private fun getName(localId: String): String {
        return AuthContext.runAs(USER) { records.getAtt(actionRef(localId), "name.en").asText() }
    }

    @Test
    fun `same id in global and two workspaces doesn't collide`() {

        createAction("act", "")
        createAction("act", WS_1)
        createAction("act", WS_2)

        assertThat(actionDao.getCount()).isEqualTo(3)

        assertThat(sysGetAction("act")!!.name.getClosest()).isEqualTo("act-")
        assertThat(sysGetAction("act")!!.workspace).isEmpty()

        val wsAction = sysGetAction("$WS_1:act")!!
        assertThat(wsAction.id).isEqualTo("act")
        assertThat(wsAction.workspace).isEqualTo(WS_1)
        assertThat(wsAction.name.getClosest()).isEqualTo("act-$WS_1")

        assertThat(sysGetAction("$WS_2:act")!!.name.getClosest()).isEqualTo("act-$WS_2")
        assertThat(sysGetAction("unknown-ws:act")).isNull()
        assertThat(sysGetAction("$WS_1:unknown")).isNull()

        // global workspaces are stored as global
        createAction("admin-act", "admin\$workspace")
        createAction("default-act", "default")
        assertThat(sysGetAction("admin-act")).isNotNull
        assertThat(sysGetAction("default-act")).isNotNull
    }

    @Test
    fun `record id and attributes of an action in workspace`() {

        userWorkspaces.add(WS_1)
        createAction("act", "")
        createAction("act", WS_1)

        AuthContext.runAs(USER) {
            val atts = records.getAtts(actionRef("$WS_1:act"), listOf("?id", "?localId", "moduleId", "workspace"))
            assertThat(atts["?id"].asText()).isEqualTo("uiserv/action@$WS_1:act")
            assertThat(atts["moduleId"].asText()).isEqualTo("act")
            assertThat(atts["workspace"].asText()).isEqualTo(WS_1)

            assertThat(records.getAtt(actionRef("act"), "?id").asText()).isEqualTo("uiserv/action@act")
        }
        assertThat(getName("$WS_1:act")).isEqualTo("act-$WS_1")
        assertThat(getName("act")).isEqualTo("act-")
    }

    @Test
    fun `action in workspace is not readable for a non member`() {

        createAction("act", WS_2)

        assertThat(getName("$WS_2:act")).isEmpty()
        assertThat(AuthContext.runAsSystem { records.getAtt(actionRef("$WS_2:act"), "name.en").asText() })
            .isEqualTo("act-$WS_2")
    }

    @Test
    fun `export doesn't contain workspace`() {

        userWorkspaces.add(WS_1)
        createAction("act", WS_1)

        val json = AuthContext.runAs(USER) { records.getAtt(actionRef("$WS_1:act"), "?json") }
        assertThat(json["id"].asText()).isEqualTo("act")
        assertThat(json.has("workspace")).isFalse()

        val data = AuthContext.runAs(USER) { records.getAtt(actionRef("$WS_1:act"), "data?str").asText() }
        assertThat(data).isNotBlank()
        assertThat(data).doesNotContain("workspace")
    }

    @Test
    fun `new action is created in the context workspace by a workspace manager`() {

        userWorkspaces.add(WS_1)
        managedWorkspaces.add(WS_1)
        createAction("act", "")

        val atts = ObjectData.create()
            .set(RecordConstants.ATT_WORKSPACE, WS_1)
            .set("moduleId", "act")
            .set("name", MLText("created-in-ws"))
            .set("type", "test-type")

        val ref = AuthContext.runAs(USER) { records.mutate(actionRef(""), atts) }

        assertThat(ref).isEqualTo(actionRef("$WS_1:act"))
        assertThat(getName("$WS_1:act")).isEqualTo("created-in-ws")
        assertThat(getName("act")).isEqualTo("act-")
    }

    @Test
    fun `workspace attribute may come before the id`() {

        managedWorkspaces.add(WS_1)
        userWorkspaces.add(WS_1)

        val atts = ObjectData.create()
        atts["id"] = "act"
        atts[RecordConstants.ATT_WORKSPACE] = WS_1
        atts["moduleId"] = "act"
        val ref = AuthContext.runAs(USER) { records.mutate(actionRef(""), atts) }
        assertThat(ref).isEqualTo(actionRef("$WS_1:act"))

        val atts2 = ObjectData.create()
        atts2[RecordConstants.ATT_WORKSPACE] = WS_1
        atts2["moduleId"] = "act2"
        val ref2 = AuthContext.runAs(USER) { records.mutate(actionRef(""), atts2) }
        assertThat(ref2).isEqualTo(actionRef("$WS_1:act2"))
    }

    @Test
    fun `non manager can't create an action in workspace`() {

        userWorkspaces.add(WS_1)

        val atts = ObjectData.create()
            .set(RecordConstants.ATT_WORKSPACE, WS_1)
            .set("moduleId", "act")

        assertThatThrownBy {
            AuthContext.runAs(USER) { records.mutate(actionRef(""), atts) }
        }.hasMessageContaining("Permission denied")

        assertThat(actionDao.getCount()).isEqualTo(0)
    }

    @Test
    fun `editing an existing action from workspace keeps its workspace`() {

        managedWorkspaces.add(WS_1)
        userWorkspaces.add(WS_1)
        createAction("act", "")
        createAction("ws-act", WS_1)

        // global action edited by admin from inside a workspace stays global
        val ref = AuthContext.runAsFull("admin", listOf("ROLE_ADMIN")) {
            records.mutate(
                actionRef("act"),
                ObjectData.create()
                    .set(RecordConstants.ATT_WORKSPACE, WS_1)
                    .set("name", MLText("changed"))
            )
        }
        assertThat(ref).isEqualTo(actionRef("act"))
        assertThat(getName("act")).isEqualTo("changed")
        assertThat(sysGetAction("$WS_1:act")).isNull()

        // action in workspace edited from the global context stays in workspace
        val wsRef = AuthContext.runAs(USER) {
            records.mutate(
                actionRef("$WS_1:ws-act"),
                ObjectData.create()
                    .set(RecordConstants.ATT_WORKSPACE, "default")
                    .set("name", MLText("changed-ws"))
            )
        }
        assertThat(wsRef).isEqualTo(actionRef("$WS_1:ws-act"))
        assertThat(getName("$WS_1:ws-act")).isEqualTo("changed-ws")
        assertThat(actionDao.getCount()).isEqualTo(2)
    }

    @Test
    fun `copy of a global action is created in the context workspace`() {

        managedWorkspaces.add(WS_1)
        userWorkspaces.add(WS_1)
        createAction("act", "")

        val ref = AuthContext.runAs(USER) {
            records.mutate(
                actionRef("act"),
                ObjectData.create()
                    .set(RecordConstants.ATT_WORKSPACE, WS_1)
                    .set("moduleId", "act-copy")
            )
        }
        assertThat(ref).isEqualTo(actionRef("$WS_1:act-copy"))
        assertThat(getName("$WS_1:act-copy")).isEqualTo("act-")
        assertThat(getName("act")).isEqualTo("act-")
    }

    @Test
    fun `delete removes only the action of the workspace`() {

        managedWorkspaces.add(WS_1)
        createAction("act", "")
        createAction("act", WS_1)

        AuthContext.runAs(USER) { records.delete(actionRef("$WS_1:act")) }

        assertThat(sysGetAction("$WS_1:act")).isNull()
        assertThat(sysGetAction("act")).isNotNull
    }

    @Test
    fun `non manager can't delete an action in workspace`() {

        createAction("act", WS_1)

        assertThatThrownBy {
            AuthContext.runAs(USER) { records.delete(actionRef("$WS_1:act")) }
        }.hasMessageContaining("Permission denied")

        assertThat(sysGetAction("$WS_1:act")).isNotNull
    }

    @Test
    fun `write permission of an action in workspace`() {

        userWorkspaces.add(WS_1)
        createAction("act", WS_1)

        val att = "permissions._has.write?bool"
        assertThat(AuthContext.runAs(USER) { records.getAtt(actionRef("$WS_1:act"), att).asBoolean() }).isFalse()

        managedWorkspaces.add(WS_1)
        assertThat(AuthContext.runAs(USER) { records.getAtt(actionRef("$WS_1:act"), att).asBoolean() }).isTrue()
    }

    @Test
    fun `query returns global actions and actions of available workspaces`() {

        userWorkspaces.add(WS_1)
        createAction("act", "")
        createAction("act", WS_1)
        createAction("act", WS_2)

        fun query(workspaces: List<String>): List<String> {
            val query = RecordsQuery.create()
                .withSourceId(ActionRecords.ID)
                .withQuery(Predicates.eq("moduleId", "act"))
                .withLanguage(PredicateService.LANGUAGE_PREDICATE)
                .withWorkspaces(workspaces)
                .build()
            return AuthContext.runAs(USER) {
                val res = records.query(query)
                assertThat(res.getTotalCount()).isEqualTo(res.getRecords().size.toLong())
                res.getRecords().map { it.getLocalId() }
            }
        }

        assertThat(query(emptyList())).containsExactlyInAnyOrder("act", "$WS_1:act")
        // explicitly requested workspaces don't include global actions (same as for journals and forms)
        assertThat(query(listOf(WS_1))).containsExactly("$WS_1:act")
        assertThat(query(listOf(WS_1, "default"))).containsExactlyInAnyOrder("act", "$WS_1:act")
        assertThat(query(listOf("default"))).containsExactly("act")
        // not a member of WS_2
        assertThat(query(listOf(WS_2))).isEmpty()
        assertThat(query(listOf(WS_2, "default"))).containsExactly("act")

        val allQuery = RecordsQuery.create()
            .withSourceId(ActionRecords.ID)
            .withQuery(Predicates.alwaysTrue())
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .build()
        val all = AuthContext.runAs(USER) { records.query(allQuery).getRecords().map { it.getLocalId() } }
        assertThat(all).containsExactlyInAnyOrder("act", "$WS_1:act")

        val allAsSystem = AuthContext.runAsSystem { records.query(allQuery).getRecords().map { it.getLocalId() } }
        assertThat(allAsSystem).containsExactlyInAnyOrder("act", "$WS_1:act", "$WS_2:act")
    }

    @Test
    fun `actions of a type in workspace are resolved for records`() {

        userWorkspaces.add(WS_1)
        createAction("act", "")
        createAction("act", WS_1)
        createAction("other", WS_1)

        val record = EntityRef.valueOf("test/rec@1")
        val requested = listOf(actionRef("$WS_1:other"), actionRef("act"), actionRef("$WS_1:act"))

        val forRecords = AuthContext.runAs(USER) { actionService.getActionsForRecords(listOf(record), requested) }
        assertThat(forRecords.actions.map { it.id }).containsExactly("$WS_1:other", "act", "$WS_1:act")
        assertThat(forRecords.actions.map { it.name.getClosest() }).containsExactly("other-$WS_1", "act-", "act-$WS_1")
        assertThat(forRecords.recordActions[record]).containsExactlyInAnyOrder("$WS_1:other", "act", "$WS_1:act")

        records.register(RecordActionsRecords(actionService))
        val recordActions = AuthContext.runAs(USER) {
            records.queryOne(
                RecordsQuery.create()
                    .withSourceId("record-actions")
                    .withQuery(ObjectData.create().set("records", listOf(record)).set("actions", requested))
                    .build(),
                listOf("actions[]?id", "records[]?num")
            )!!
        }
        assertThat(recordActions["actions[]?id"].asStrList()).hasSize(3)
        assertThat(recordActions["records[]?num"].map { it.asLong() }).containsExactly(0b111L)

        // action query by records keeps the requested order and doesn't mix up actions with the same id
        val res = AuthContext.runAs(USER) {
            records.query(
                RecordsQuery.create()
                    .withSourceId(ActionRecords.ID)
                    .withQuery(ObjectData.create().set("records", listOf(record.toString())).set("actions", requested))
                    .build(),
                listOf("actions[]?json")
            )
        }
        val actions = res.getRecords()[0]["actions[]?json"]
        assertThat(actions.map { it["id"].asText() }).containsExactly("$WS_1:other", "act", "$WS_1:act")
        assertThat(actions.map { it["name"]["en"].asText() }).containsExactly("other-$WS_1", "act-", "act-$WS_1")
    }

    @Test
    fun `artifact handler deploys actions into workspace`() {

        val handler = ActionArtifactHandler(actionService)
        val changes = mutableListOf<Pair<ActionDto, String>>()
        handler.listenChanges { action, ws -> changes.add(Json.mapper.copy(action)!! to ws) }

        val artifact = Json.mapper.read("""{"id":"act","type":"test-type","name":{"en":"from-app"}}""", ActionDto::class.java)!!

        AuthContext.runAsSystem {
            handler.deployArtifact(artifact, "")
            handler.deployArtifact(artifact, WS_1)
        }

        assertThat(sysGetAction("act")).isNotNull
        assertThat(sysGetAction("$WS_1:act")!!.name.getClosest()).isEqualTo("from-app")
        assertThat(sysGetAction(IdInWs.create(WS_1, "act"))).isNotNull
        assertThat(artifact.workspace).isEmpty()

        assertThat(changes.map { it.second }).containsExactly("", WS_1)
        assertThat(changes.map { it.first.workspace }).containsOnly("")
        assertThat(changes.map { it.first.id }).containsOnly("act")

        AuthContext.runAsSystem { handler.deleteArtifact("act", WS_1) }
        assertThat(sysGetAction("$WS_1:act")).isNull()
        assertThat(sysGetAction("act")).isNotNull
    }

    @Test
    fun `id with workspace prefix creates an action in this workspace`() {

        managedWorkspaces.add(WS_1)
        userWorkspaces.add(WS_1)

        val ref = AuthContext.runAs(USER) {
            records.mutate(actionRef(""), ObjectData.create().set("moduleId", "$WS_1:act").set("type", "t"))
        }
        assertThat(ref).isEqualTo(actionRef("$WS_1:act"))
        assertThat(sysGetAction("$WS_1:act")!!.id).isEqualTo("act")

        // same workspace in the context
        val ref2 = AuthContext.runAs(USER) {
            records.mutate(
                actionRef(""),
                ObjectData.create()
                    .set("moduleId", "$WS_1:act2")
                    .set(RecordConstants.ATT_WORKSPACE, WS_1)
            )
        }
        assertThat(ref2).isEqualTo(actionRef("$WS_1:act2"))

        managedWorkspaces.add(WS_2)
        assertThatThrownBy {
            AuthContext.runAs(USER) {
                records.mutate(
                    actionRef(""),
                    ObjectData.create()
                        .set("moduleId", "$WS_1:act3")
                        .set(RecordConstants.ATT_WORKSPACE, WS_2)
                )
            }
        }.hasMessageContaining("doesn't match")

        assertThat(actionDao.getCount()).isEqualTo(2)
    }
}
