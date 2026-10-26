package ru.citeck.ecos.uiserv.domain.action.api.records

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.uiserv.domain.action.dto.ActionConfirmDef
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.eapps.ActionArtifactHandler
import ru.citeck.ecos.uiserv.domain.action.testutils.ActionsTestBase
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.Base64

class ActionWorkspaceFormsTest : ActionsTestBase() {

    @BeforeEach
    fun mapWorkspaceIds() {
        workspaceSystemIds["source"] = "source-sys-id"
        workspaceSystemIds["target"] = "target-sys-id"
    }

    @Test
    fun `records export and content upload rebind form references in both attribute orders`() {
        for ((index, id) in formIds("source-sys-id:").withIndex()) {
            val action = action("edit-$index", id).apply { workspace = "source" }
            AuthContext.runAsSystem { actionService.updateAction(action) }
            val before = Json.mapper.toString(action)
            val sourceRef = EntityRef.valueOf("action@source-sys-id:edit-$index")

            AuthContext.runAsSystem {
                val exported = records.getAtt(sourceRef, "?json")
                assertThat(exported["config"]["formId"].asText()).isEqualTo(formIds("CURRENT_WS:")[index])
                assertThat(exported["confirm"]["formRef"].asText()).isEqualTo("uiserv/form@CURRENT_WS:confirm")
                assertThat(exported.has("workspace")).isFalse()

                val bytes = Base64.getDecoder().decode(records.getAtt(sourceRef, "data").asText())
                val data = requireNotNull(Json.mapper.convert(YamlUtils.read(String(bytes, Charsets.UTF_8)), ObjectData::class.java))
                assertThat(data["config"]["formId"].asText()).isEqualTo(formIds("CURRENT_WS:")[index])
                assertThat(data["confirm"]["formRef"].asText()).isEqualTo("uiserv/form@CURRENT_WS:confirm")

                for (contentFirst in listOf(true, false)) {
                    data["id"] = "uploaded-$index-$contentFirst"
                    val url = "data:application/json;base64," +
                        Base64.getEncoder().encodeToString(Json.mapper.toBytesNotNull(data))
                    val content = "_content" to listOf(mapOf("url" to url))
                    val ws = "_workspace" to "emodel/workspace@target"
                    val attributes: Map<String, Any>
                    if (contentFirst) {
                        attributes = linkedMapOf(content, ws)
                    } else {
                        attributes = linkedMapOf(ws, content)
                    }
                    val atts = ObjectData.create(attributes)
                    val ref = records.mutate(EntityRef.valueOf("action@"), atts)
                    assertThat(ref.getLocalId()).isEqualTo("target-sys-id:uploaded-$index-$contentFirst")
                    val saved = requireNotNull(actionService.getAction(IdInWs.create("target", "uploaded-$index-$contentFirst")))
                    assertForms(saved, formIds("target-sys-id:")[index], "target-sys-id:")
                    assertThat(saved.config["label"].asText()).isEqualTo("source-sys-id:literal")
                    assertThat(saved.config["attributes"]["formId"].asText()).isEqualTo("target-sys-id:literal")
                }
                assertThat(Json.mapper.toString(actionService.getAction(sourceRef.getLocalId()))).isEqualTo(before)
            }
        }
    }

    @Test
    fun `artifact export and deployment rebind forms without mutating the original artifact`() {
        val handler = ActionArtifactHandler(actionService, workspaceService)
        val exports = mutableListOf<Pair<ActionDto, String>>()
        handler.listenChanges { action, ws -> exports.add(action to ws) }

        for ((index, id) in formIds("source-sys-id:").withIndex()) {
            val source = action("artifact-$index", id).apply { workspace = "source" }
            AuthContext.runAsSystem { actionService.updateAction(source) }
            val exported = exports.last()
            assertThat(exported.second).isEqualTo("source")
            assertThat(exported.first.workspace).isEmpty()
            assertForms(exported.first, formIds("CURRENT_WS:")[index], "CURRENT_WS:")

            val snapshot = Json.mapper.toString(exported.first)
            AuthContext.runAsSystem { handler.deployArtifact(exported.first, "target") }
            val saved = requireNotNull(AuthContext.runAsSystem { actionService.getAction(IdInWs.create("target", source.id)) })
            assertForms(saved, formIds("target-sys-id:")[index], "target-sys-id:")
            assertThat(Json.mapper.toString(exported.first)).isEqualTo(snapshot)
            assertForms(source, id, "source-sys-id:")
        }
    }

    @Test
    fun `artifact deployment and export keep the whole confirm config`() {
        val handler = ActionArtifactHandler(actionService, workspaceService)
        val exports = mutableListOf<ActionDto>()
        handler.listenChanges { action, _ -> exports.add(action) }

        AuthContext.runAsSystem {
            val global = action("confirm-global", "global-form").apply { confirm = confirm("uiserv/form@global-confirm") }
            handler.deployArtifact(global, "")
            assertThat(requireNotNull(actionService.getAction("confirm-global")).confirm)
                .isEqualTo(confirm("uiserv/form@global-confirm"))

            val source = action("confirm-ws", "source-sys-id:edit").apply {
                workspace = "source"
                confirm = confirm("uiserv/form@source-sys-id:confirm")
            }
            actionService.updateAction(source)
            val exported = exports.last()
            assertThat(exported.confirm).isEqualTo(confirm("uiserv/form@CURRENT_WS:confirm"))

            handler.deployArtifact(exported, "target")
            assertThat(requireNotNull(actionService.getAction(IdInWs.create("target", source.id))).confirm)
                .isEqualTo(confirm("uiserv/form@target-sys-id:confirm"))
        }
    }

    @Test
    fun `unqualified form ids stay global and placeholders imported globally lose the prefix`() {
        val handler = ActionArtifactHandler(actionService, workspaceService)
        AuthContext.runAsSystem {
            for (id in listOf("global-form", "uiserv/form@global-form", "123", "true")) {
                val artifact = action("global", id)
                handler.deployArtifact(artifact, "target")
                assertThat(requireNotNull(actionService.getAction(IdInWs.create("target", "global"))).config["formId"].asText())
                    .isEqualTo(id)
            }
            handler.deployArtifact(action("global-import", "CURRENT_WS:edit"), "")
            assertForms(requireNotNull(actionService.getAction("global-import")), "edit", "")
        }
    }

    private fun formIds(prefix: String): List<String> {
        return listOf(
            "${prefix}edit",
            "uiserv/form@${prefix}edit",
            "type${'$'}${prefix}record",
            "uiserv/form@type${'$'}${prefix}record"
        )
    }

    private fun action(id: String, formId: String): ActionDto {
        val action = ActionDto()
        action.id = id
        action.type = "edit"
        action.config = ObjectData.create().set("formId", formId).set("label", "source-sys-id:literal")
            .set("attributes", mapOf("formId" to "source-sys-id:literal"))
        var prefix = "source-sys-id:"
        if (formId.contains("CURRENT_WS:")) {
            prefix = "CURRENT_WS:"
        }
        action.confirm = ActionConfirmDef.create().withFormRef(EntityRef.valueOf("uiserv/form@${prefix}confirm")).build()
        return action
    }

    private fun confirm(formRef: String): ActionConfirmDef {
        return ActionConfirmDef.create()
            .withTitle(MLText("Confirm"))
            .withMessage(MLText("Are you sure?"))
            .withFormRef(EntityRef.valueOf(formRef))
            .withFormAttributes(ObjectData.create().set("mode", "short"))
            .withAttributesMapping(mapOf("config.reason" to "reason"))
            .build()
    }

    private fun assertForms(action: ActionDto, formId: String, prefix: String) {
        assertThat(action.config["formId"].asText()).isEqualTo(formId)
        assertThat(action.confirm.formRef.toString()).isEqualTo("uiserv/form@${prefix}confirm")
    }
}
