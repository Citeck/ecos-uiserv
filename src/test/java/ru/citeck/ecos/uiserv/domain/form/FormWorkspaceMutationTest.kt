package ru.citeck.ecos.uiserv.domain.form

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.ModelServiceFactory
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.uiserv.domain.form.api.records.EcosFormMutRecord
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.Base64

class FormWorkspaceMutationTest : FormsTestBase() {

    @Test
    fun testJacksonAppliesWorkspaceAttribute() {
        val workspaceService = ModelServiceFactory().workspaceService

        // Simulate the mutation flow: blank record, apply attributes via Jackson
        val record = EcosFormMutRecord(workspaceService)

        val data = ObjectData.create()
        data["id"] = "custom-form"
        data[RecordConstants.ATT_WORKSPACE] = "some-ws"
        data["formKey"] = "test-key"

        Json.mapper.applyData(record, data)

        assertEquals("custom-form", record.id, "id should be set")
        assertEquals("some-ws", record.workspace, "_workspace should be applied to workspace")
        assertEquals("test-key", record.formKey, "formKey should be set")
    }

    @Test
    fun testJacksonAppliesWorkspaceOnExistingForm() {
        val workspaceService = ModelServiceFactory().workspaceService

        // Simulate existing global form (workspace = DEFAULT)
        val existingForm = EcosFormDef.create()
            .withId("existing-form")
            .withWorkspace("DEFAULT")
            .withFormKey("old-key")
            .build()

        val record = EcosFormMutRecord(existingForm, workspaceService)

        val data = ObjectData.create()
        data[RecordConstants.ATT_WORKSPACE] = "some-ws"
        data["formKey"] = "new-key"

        Json.mapper.applyData(record, data)

        // For existing form with non-blank workspace, getUpdatedWsInMutation
        // should keep the original workspace (since currentWs is not blank)
        assertEquals("existing-form", record.id)
        assertEquals("DEFAULT", record.workspace, "existing form workspace should be preserved")
        assertEquals("new-key", record.formKey)
    }

    @Test
    fun testJacksonAppliesWorkspaceOnBlankExistingForm() {
        val workspaceService = ModelServiceFactory().workspaceService

        // Simulate existing form with blank workspace
        val existingForm = EcosFormDef.create()
            .withId("existing-form")
            .withWorkspace("")
            .build()

        val record = EcosFormMutRecord(existingForm, workspaceService)

        val data = ObjectData.create()
        data[RecordConstants.ATT_WORKSPACE] = "some-ws"

        Json.mapper.applyData(record, data)

        // For existing form with blank workspace and non-global ctxWorkspace,
        // getUpdatedWsInMutation should apply the new workspace
        assertEquals("existing-form", record.id)
        assertEquals(
            "some-ws",
            record.workspace,
            "_workspace should be applied when current workspace is blank"
        )
    }

    @Test
    fun testMutateFormInWorkspaceWithSchemaFormat() {
        // Simulate the exact production request format with ?str suffixes
        val globalForm = EcosFormDef.create()
            .withId("ecos-vacation-form")
            .withFormKey("vacation")
            .build()
        ecosFormService.save(globalForm)

        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            // Use ?str suffix format matching the real HTTP request
            val mutAtts = RecordAtts(EntityRef.create("", "form", ""))
            mutAtts.setAtt("id?str", "ecos-vacation-form")
            mutAtts.setAtt("_workspace?str", "corpport-workspace")
            mutAtts.setAtt("formKey?str", "vacation-ws")

            val result = recordsService.mutate(mutAtts)
            assertNotNull(result)

            // Verify the return ref contains workspace prefix
            val localId = result.getLocalId()
            assertEquals(
                "corpport-workspace:ecos-vacation-form",
                localId,
                "Return ref should contain workspace prefix. Actual: $localId"
            )
        }

        // Check that global form was NOT modified
        val globalFormAfter = ecosFormService.getFormById(IdInWs.create("ecos-vacation-form"))
        assertNotNull(globalFormAfter.orElse(null), "Global form should still exist")
        assertEquals(
            "vacation",
            globalFormAfter.get().formKey,
            "Global form should NOT be mutated"
        )

        // Check that workspace form was created
        val wsForm = ecosFormService.getFormById(IdInWs.create("corpport-workspace", "ecos-vacation-form"))
        assertNotNull(wsForm.orElse(null), "Workspace form should be created")
        assertEquals("vacation-ws", wsForm.get().formKey)
        assertEquals("corpport-workspace", wsForm.get().workspace)
    }

    @Test
    fun testWorkspaceOverwrittenBySelfExpansion() {
        // Reproduces the bug: when mutation uses _self content-data,
        // the form JSON's "workspace" field overwrites the _workspace value

        // Create a global form first
        val globalForm = EcosFormDef.create()
            .withId("ecos-vacation-form")
            .withFormKey("vacation")
            .build()
        ecosFormService.save(globalForm)

        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            // Build form JSON (as it would come from a global form - workspace is "")
            val formJson = """{"id":"ecos-vacation-form","formKey":"vacation-ws","workspace":""}"""
            val formBytes = formJson.toByteArray()

            val atts = RecordAtts(EntityRef.create("", "form", ""))
            atts[RecordConstants.ATT_WORKSPACE] = "corpport-workspace"
            atts[".att(n:\"_self\"){as(n:\"content-data\"){json}}"] =
                ObjectData.create()
                    .set("storage", "base64")
                    .set("name", "form.json")
                    .set(
                        "url",
                        "data:application/json;base64," + Base64.getEncoder().encodeToString(formBytes)
                    )
                    .set("size", formBytes.size)
                    .set("type", "application/json")
                    .set("originalName", "form.json")

            val result = recordsService.mutate(atts)
            assertNotNull(result)

            // Verify the return ref contains workspace prefix
            val localId = result.getLocalId()
            assertEquals(
                "corpport-workspace:ecos-vacation-form",
                localId,
                "Return ref should contain workspace prefix. Actual: $localId"
            )
        }

        // Check that global form was NOT modified
        val globalFormAfter = ecosFormService.getFormById(IdInWs.create("ecos-vacation-form"))
        assertNotNull(globalFormAfter.orElse(null), "Global form should still exist")
        assertEquals(
            "vacation",
            globalFormAfter.get().formKey,
            "Global form should NOT be mutated"
        )

        // Check that workspace form was created
        val wsForm = ecosFormService.getFormById(IdInWs.create("corpport-workspace", "ecos-vacation-form"))
        assertNotNull(wsForm.orElse(null), "Workspace form should be created")
        assertEquals("vacation-ws", wsForm.get().formKey)
        assertEquals("corpport-workspace", wsForm.get().workspace)
    }

    @Test
    fun testMutateFormInWorkspaceViaRecordsApi() {
        // First, create a global form
        val globalForm = EcosFormDef.create()
            .withId("vacation-request-form")
            .withFormKey("vacation")
            .withWorkspace("default")
            .build()
        ecosFormService.save(globalForm)

        // Verify global form exists
        val globalFormCheck = ecosFormService.getFormById(IdInWs.create("vacation-request-form"))
        assertNotNull(globalFormCheck.orElse(null), "Global form should exist")

        // Now send mutation through Records API with empty recordId + _workspace
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val mutAtts = RecordAtts(EntityRef.create("", "form", ""))
            mutAtts.setAtt("id", "vacation-request-form")
            mutAtts.setAtt(RecordConstants.ATT_WORKSPACE, "some-ws")
            mutAtts.setAtt("formKey", "vacation-ws")

            val result = recordsService.mutate(mutAtts)

            // Check that the mutation result is not null
            assertNotNull(result)
        }

        // Check that global form was NOT modified
        val globalFormAfter = ecosFormService.getFormById(IdInWs.create("vacation-request-form"))
        assertNotNull(globalFormAfter.orElse(null), "Global form should still exist")
        assertEquals(
            "vacation",
            globalFormAfter.get().formKey,
            "Global form should NOT be mutated - its formKey should remain 'vacation'"
        )

        // Check that workspace form was created
        val wsForm = ecosFormService.getFormById(IdInWs.create("some-ws", "vacation-request-form"))
        assertNotNull(wsForm.orElse(null), "Workspace form should be created")
        assertEquals(
            "vacation-ws",
            wsForm.get().formKey,
            "Workspace form should have the new formKey"
        )
        assertEquals("some-ws", wsForm.get().workspace, "Workspace form should have correct workspace")
    }
}
