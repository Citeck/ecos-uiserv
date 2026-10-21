package ru.citeck.ecos.uiserv.domain.action.api.records

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.eapps.ActionArtifactHandler
import ru.citeck.ecos.uiserv.domain.action.service.ActionWorkspaceRefs
import ru.citeck.ecos.uiserv.domain.action.testutils.ActionsTestBase
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorDto
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.Base64

class ActionWorkspaceConfigRefsTest : ActionsTestBase() {

    @BeforeEach
    fun mapWorkspaceIds() {
        workspaceSystemIds["source"] = "source-sys-id"
        workspaceSystemIds["target"] = "target-sys-id"
        workspaceSystemIds["foreign"] = "foreign-sys-id"
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun `static references round trip through Records and artifacts`(case: RefCase) {
        val source = ActionDto().apply {
            id = "refs"
            type = case.type
            workspace = "source"
            preActionModule = "source-sys-id:js-module"
            predicate = predicate("source-sys-id:")
            evaluator = evaluator("source-sys-id:")
            config = case.config("source-sys-id:")
        }
        val snapshot = Json.mapper.toString(source)
        val handler = ActionArtifactHandler(actionService, workspaceService)
        val exports = mutableListOf<ActionDto>()
        handler.listenChanges { action, _ -> exports.add(action) }

        AuthContext.runAsSystem {
            actionService.updateAction(source)
            assertPortable(exports.single(), case)
            val ref = EntityRef.valueOf("action@source-sys-id:refs")
            val exportedJson = records.getAtt(ref, "?json")
            assertPortable(requireNotNull(Json.mapper.convert(exportedJson, ActionDto::class.java)), case)

            val bytes = Base64.getDecoder().decode(records.getAtt(ref, "data").asText())
            val exported = requireNotNull(Json.mapper.convert(YamlUtils.read(String(bytes, Charsets.UTF_8)), ActionDto::class.java))
            assertPortable(exported, case)

            val portableSnapshot = Json.mapper.toString(exported)
            handler.deployArtifact(exported, "target")
            assertBound(requireNotNull(actionService.getAction(IdInWs.create("target", "refs"))), case)
            assertThat(Json.mapper.toString(exported)).isEqualTo(portableSnapshot)

            val url = "data:application/json;base64," +
                Base64.getEncoder().encodeToString(Json.mapper.toBytesNotNull(exported))
            records.mutate(
                EntityRef.valueOf("action@"),
                ObjectData.create().set("_workspace", "target").set("_content", listOf(mapOf("url" to url)))
            )
            assertBound(requireNotNull(actionService.getAction(IdInWs.create("target", "refs"))), case)
            assertThat(Json.mapper.toString(source)).isEqualTo(snapshot)
            assertThat(Json.mapper.toString(actionService.getAction(IdInWs.create("source", "refs"))))
                .isEqualTo(snapshot)
        }
    }

    @Test
    fun `predicate mapping preserves composite conditions and source values`() {
        val action = ActionDto().apply {
            workspace = "source"
            predicate = Json.mapper.readNotNull(
                """{"t":"or","val":[{"t":"and","val":[{"t":"eq","att":"_type","val":"emodel/type@source-sys-id:contract"}]},{"t":"not","val":{"t":"in","att":"_type?localId","val":["source-sys-id:hidden","foreign-sys-id:hidden","global","${'$'}{_type?localId}"]}},{"t":"contains","att":"_type?id","val":"emodel/type@source-sys-id:other"},{"t":"eq","att":"name","val":"source-sys-id:literal"}]}""",
                Predicate::class.java
            )
        }
        val snapshot = requireNotNull(Json.mapper.toString(action))
        val exported = ActionWorkspaceRefs.prepareForExport(action, workspaceService)
        val expectedExport = snapshot.replace("source-sys-id:contract", "CURRENT_WS:contract")
            .replace("source-sys-id:hidden", "CURRENT_WS:hidden")
            .replace("source-sys-id:other", "CURRENT_WS:other")
        assertThat(Json.mapper.toString(exported)).isEqualTo(expectedExport)
        val imported = ActionWorkspaceRefs.bindToWorkspace(exported, "target", workspaceService)
        assertThat(Json.mapper.toString(imported)).isEqualTo(expectedExport.replace("CURRENT_WS:", "target-sys-id:"))
        assertThat(Json.mapper.toString(action)).isEqualTo(snapshot)
    }

    @Test
    fun `predicate mapping retains empty nested groups`() {
        val action = ActionDto().apply {
            workspace = "source"
            predicate = Json.mapper.readNotNull(
                """{"t":"or","val":[{"t":"and","val":[]},{"t":"not","val":{"t":"or","val":[]}},{"t":"eq","att":"_type","val":"emodel/type@source-sys-id:contract"}]}""",
                Predicate::class.java
            )
        }
        val snapshot = requireNotNull(Json.mapper.toString(action))
        val exported = ActionWorkspaceRefs.prepareForExport(action, workspaceService)
        assertThat(Json.mapper.toString(exported)).isEqualTo(snapshot.replace("source-sys-id:", "CURRENT_WS:"))
        assertThat(Json.mapper.toString(action)).isEqualTo(snapshot)
    }

    @Test
    fun `global actions keep explicit workspace refs and import only binds placeholders`() {
        val action = ActionDto().apply {
            id = "global"
            type = "custom-handler"
            typeRef = EntityRef.valueOf("emodel/type@source-sys-id:unused")
            config = ObjectData.create(
                """{"formId":"source-sys-id:shared","formRef":"uiserv/form@type${'$'}foreign-sys-id:shared","nested":{"typeRef":"emodel/type@CURRENT_WS:local"}}"""
            )
        }
        val before = Json.mapper.toString(action)
        val exported = ActionWorkspaceRefs.prepareForExport(action, workspaceService)
        assertThat(Json.mapper.toString(exported)).isEqualTo(before)
        val imported = ActionWorkspaceRefs.bindToWorkspace(exported, "target", workspaceService)
        assertThat(imported.config["formId"].asText()).isEqualTo("source-sys-id:shared")
        assertThat(imported.config["formRef"].asText()).isEqualTo("uiserv/form@type${'$'}foreign-sys-id:shared")
        assertThat(imported.config["nested"]["typeRef"].asText()).isEqualTo("emodel/type@target-sys-id:local")
        assertThat(imported.typeRef).isEqualTo(action.typeRef)
        assertThat(Json.mapper.toString(action)).isEqualTo(before)
    }

    private fun assertPortable(action: ActionDto, case: RefCase) {
        assertThat(action.workspace).isEmpty()
        assertThat(action.config).isEqualTo(case.config("CURRENT_WS:"))
        assertThat(action.preActionModule).isEqualTo("source-sys-id:js-module")
        assertThat(action.predicate).isEqualTo(predicate("CURRENT_WS:"))
        assertThat(Json.mapper.toString(action.evaluator)).isEqualTo(Json.mapper.toString(evaluator("CURRENT_WS:")))
    }

    private fun assertBound(action: ActionDto, case: RefCase) {
        assertThat(action.workspace).isEqualTo("target")
        assertThat(action.config).isEqualTo(case.config("target-sys-id:"))
        assertThat(action.preActionModule).isEqualTo("source-sys-id:js-module")
        assertThat(action.predicate).isEqualTo(predicate("target-sys-id:"))
        assertThat(Json.mapper.toString(action.evaluator)).isEqualTo(Json.mapper.toString(evaluator("target-sys-id:")))
    }

    private fun predicate(prefix: String): Predicate {
        return Json.mapper.readNotNull(
            """{"t":"and","val":[{"t":"eq","att":"_type","val":"emodel/type@${prefix}contract"},{"t":"not","val":{"t":"in","att":"_type?localId","val":["${prefix}hidden","global"]}},{"t":"contains","att":"_type?id","val":["emodel/type@${prefix}other","${'$'}{_type?id}"]},{"t":"eq","att":"name","val":"emodel/type@source-sys-id:literal"},{"t":"like","att":"_type","val":"emodel/type@source-sys-id:pattern*"}]}""",
            Predicate::class.java
        )
    }

    private fun evaluator(prefix: String): RecordEvaluatorDto {
        return Json.mapper.readNotNull(
            """{"type":"group","config":{"joinBy":"AND","evaluators":[{"type":"predicate","config":{"predicate":${Json.mapper.toString(predicate(prefix))}}},{"type":"has-attribute","config":{"attribute":"source-sys-id:literal"}}]}}""",
            RecordEvaluatorDto::class.java
        )
    }

    data class RefCase(val name: String, val type: String, val json: String) {
        fun config(prefix: String): ObjectData {
            return ObjectData.create(json.replace("__WS__", prefix))
                .set(
                    "foreign",
                    mapOf(
                        "formId" to "foreign-sys-id:shared",
                        "typeRef" to "emodel/type@foreign-sys-id:shared",
                        "nested" to listOf(mapOf("formRef" to "uiserv/form@type${'$'}foreign-sys-id:shared"))
                    )
                )
        }

        override fun toString(): String {
            return name
        }
    }

    companion object {
        @JvmStatic
        fun cases(): List<RefCase> {
            return listOf(
                RefCase(
                    "create type and full variant",
                    "create",
                    """{"typeRef":"emodel/type@__WS__contract","createVariant":{"id":"variant","typeRef":"emodel/type@__WS__variant-type","formRef":"uiserv/form@type${'$'}__WS__contract","formId":"__WS__form","postActionRef":"uiserv/action@__WS__post","recordRef":"emodel/type@__WS__record","destination":"emodel/type@__WS__parent","sourceId":"emodel/source-sys-id:source","attributes":{"typeRef":"emodel/type@__WS__literal"}},"attributes":{"typeRef":"emodel/type@__WS__literal"}}"""
                ),
                RefCase("legacy create", "create", """{"recordRef":"emodel/type@__WS__contract","formId":"__WS__form"}"""),
                RefCase("edit target and reset list", "edit", """{"recordId":"uiserv/form@__WS__form","recordsToReset":["emodel/type@__WS__contract","emodel/type@global",123,"${'$'}{?id}"]}"""),
                RefCase("JSON editor form", "edit-json", """{"editorFormId":"uiserv/form@__WS__editor"}"""),
                RefCase("journal preset", "edit-journal-preset", """{"data":{"journalId":"type${'$'}__WS__contract","workspaces":["source"],"settings":{"typeRef":"emodel/type@__WS__literal"}}}"""),
                RefCase("debug target", "debug-form", """{"recordRef":"emodel/type@__WS__contract"}"""),
                RefCase("template download", "download-by-template", """{"templateRef":"transformations/template@__WS__template","requestParams":{"templateRef":"__WS__literal"}}"""),
                RefCase("task form", "task-outcome", """{"formRef":"uiserv/form@__WS__form","taskRef":"eproc/wftask@__WS__task"}"""),
                RefCase("task overrides", "tasks-actions", """{"formRef":"uiserv/form@__WS__form"}"""),
                RefCase("mutation target", "mutate", """{"record":{"id":"emodel/type@__WS__contract","attributes":{"_type":"emodel/type@__WS__literal"}}}"""),
                RefCase("event target", "user-event", """{"record":"emodel/type@__WS__contract","eventData":{"typeRef":"emodel/type@__WS__literal"}}"""),
                RefCase(
                    "nested assoc actions",
                    "assoc-action",
                    """{"assoc":"some-association","action":{"type":"assoc-action","typeRef":"emodel/type@__WS__legacy-unused","predicate":{"t":"eq","att":"_type","val":"emodel/type@__WS__contract"},"confirm":{"formRef":"uiserv/form@__WS__confirm"},"extra":"source-sys-id:literal","config":{"action":{"type":"create","config":{"typeRef":"emodel/type@__WS__contract","createVariant":{"postActionRef":"uiserv/action@__WS__post"}}}}}}"""
                ),
                RefCase(
                    "transformation inputs and outputs",
                    "transform",
                    """{"input":{"type":"template","config":{"templateRef":"transformations/template@__WS__template","entityRef":"emodel/type@__WS__contract"}},"output":{"type":"mutate","config":{"entityRef":"emodel/type@__WS__target","additionalAttributes":{"typeRef":"emodel/type@__WS__literal"}}},"transformations":[{"type":"merge","config":{"sources":[{"type":"entity-content","config":{"entityRef":"emodel/type@__WS__other"}},{"type":"template","config":{"templateRef":"transformations/template@__WS__part","entityRef":"emodel/type@__WS__contract"}}]}},{"type":"barcode","config":{"entityRef":"emodel/type@__WS__barcode"}},{"type":"applyImage","config":{"entityRef":"emodel/type@__WS__image","imageUrl":"https://host/source-sys-id:image"}}]}"""
                ),
                RefCase(
                    "expressions and text stay intact while nested reference keys rebind",
                    "create",
                    """{"typeRef":"emodel/type@source-sys-id:${'$'}{typeId}","formId":"{{formRef}}","createVariant":{"typeRef":"${'$'}{_type?id}","formRef":"uiserv/form@source-sys-id:{{form}}"},"options":{"typeRef":"emodel/type@__WS__literal"},"attributes":{"formId":"__WS__literal","nested":{"typeRef":"emodel/type@__WS__literal"}},"label":"source-sys-id:literal"}"""
                ),
                RefCase("global refs", "create", """{"typeRef":"emodel/type@global","formId":"global-form","createVariant":{"formRef":"uiserv/form@global","postActionRef":"uiserv/action@global"}}"""),
                RefCase("custom action reference keys", "custom-action", """{"typeRef":"emodel/type@__WS__literal","action":{"type":"create","config":{"typeRef":"emodel/type@__WS__literal"}},"script":"return 'source-sys-id:literal';"}"""),
                RefCase(
                    "deep custom arrays and plural reference keys",
                    "another-custom-handler",
                    """{"unknownContainer":[[{"formIds":["__WS__form",["uiserv/form@type${'$'}__WS__auto",null,123,true]],"typeRefs":["emodel/type@__WS__type","emodel/type@foreign-sys-id:type"],"journalRefs":["uiserv/journal@__WS__journal"],"actionRefs":["__WS__action"],"ordinaryText":["source-sys-id:literal","uiserv/form@source-sys-id:text"]}]],"record":{"id":"emodel/type@__WS__record","module":"source-sys-id:module"}}"""
                ),
                RefCase("malformed optional config", "create", """{"typeRef":123,"formId":false,"createVariant":null}"""),
                RefCase("missing optional config", "create", "{}")
            )
        }
    }
}
