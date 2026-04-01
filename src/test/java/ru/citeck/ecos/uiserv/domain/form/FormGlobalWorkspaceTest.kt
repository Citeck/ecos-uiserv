package ru.citeck.ecos.uiserv.domain.form

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Tests that global workspaces (admin$workspace, default, blank)
 * are normalized to empty string when saving forms,
 * so that forms remain accessible without a workspace prefix in the record ID.
 */
class FormGlobalWorkspaceTest : FormsTestBase() {

    @Test
    fun `form saved with admin workspace should be found with empty workspace`() {
        val form = EcosFormDef.create()
            .withId("test-global-form")
            .withFormKey("test-key")
            .withWorkspace("admin\$workspace")
            .build()
        ecosFormService.save(form)

        val found = ecosFormService.getFormById(IdInWs.create("test-global-form"))
        assertTrue(found.isPresent, "Form saved with admin\$workspace should be findable with empty workspace")
        assertEquals("test-key", found.get().formKey)
    }

    @Test
    fun `form saved with default workspace should be found with empty workspace`() {
        val form = EcosFormDef.create()
            .withId("test-default-form")
            .withFormKey("default-key")
            .withWorkspace("default")
            .build()
        ecosFormService.save(form)

        val found = ecosFormService.getFormById(IdInWs.create("test-default-form"))
        assertTrue(found.isPresent, "Form saved with 'default' workspace should be findable with empty workspace")
        assertEquals("default-key", found.get().formKey)
    }

    @Test
    fun `form saved with regular workspace should NOT be found with empty workspace`() {
        val form = EcosFormDef.create()
            .withId("test-ws-form")
            .withFormKey("ws-key")
            .withWorkspace("corpport-workspace")
            .build()
        ecosFormService.save(form)

        val foundEmpty = ecosFormService.getFormById(IdInWs.create("test-ws-form"))
        assertFalse(foundEmpty.isPresent, "Form in regular workspace should NOT be found with empty workspace")

        val foundWs = ecosFormService.getFormById(IdInWs.create("corpport-workspace", "test-ws-form"))
        assertTrue(foundWs.isPresent, "Form in regular workspace should be found with correct workspace")
        assertEquals("ws-key", foundWs.get().formKey)
    }

    @Test
    fun `mutate form via Records API with admin workspace should return accessible ID`() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val mutAtts = RecordAtts(EntityRef.create("", "form", ""))
            mutAtts.setAtt("id?str", "admin-ws-form")
            mutAtts.setAtt("_workspace?str", "admin\$workspace")
            mutAtts.setAtt("formKey?str", "admin-key")

            val result = recordsService.mutate(mutAtts)
            assertNotNull(result)

            // ID should NOT contain workspace prefix since admin$workspace is global
            val localId = result.getLocalId()
            assertEquals("admin-ws-form", localId, "Global workspace should not produce a prefix in the ID")

            // Form should be accessible via the returned ID
            val atts = recordsService.getAtts(result, mapOf("formKey" to "formKey?str"))
            assertEquals("admin-key", atts["formKey"].asText())
        }
    }

    @Test
    fun `mutate form via Records API with regular workspace should return prefixed ID`() {
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val mutAtts = RecordAtts(EntityRef.create("", "form", ""))
            mutAtts.setAtt("id?str", "regular-ws-form")
            mutAtts.setAtt("_workspace?str", "corpport-workspace")
            mutAtts.setAtt("formKey?str", "corp-key")

            val result = recordsService.mutate(mutAtts)
            assertNotNull(result)

            val localId = result.getLocalId()
            assertEquals(
                "corpport-workspace:regular-ws-form",
                localId,
                "Regular workspace should produce a prefixed ID"
            )
        }

        // Verify via service layer (Records API read requires workspace membership mock)
        val wsForm = ecosFormService.getFormById(IdInWs.create("corpport-workspace", "regular-ws-form"))
        assertTrue(wsForm.isPresent, "Form should exist in workspace")
        assertEquals("corp-key", wsForm.get().formKey)
    }

    @Test
    fun `updating form in admin workspace should not create duplicate`() {
        val form = EcosFormDef.create()
            .withId("dup-test-form")
            .withFormKey("original-key")
            .withWorkspace("admin\$workspace")
            .build()
        ecosFormService.save(form)

        val updated = EcosFormDef.create()
            .withId("dup-test-form")
            .withFormKey("updated-key")
            .withWorkspace("admin\$workspace")
            .build()
        ecosFormService.save(updated)

        val found = ecosFormService.getFormById(IdInWs.create("dup-test-form"))
        assertTrue(found.isPresent)
        assertEquals("updated-key", found.get().formKey, "Form should be updated, not duplicated")
    }

    @Test
    fun `getRecordAtts should return form saved with admin workspace`() {
        val form = EcosFormDef.create()
            .withId("atts-test-form")
            .withFormKey("atts-key")
            .withWorkspace("admin\$workspace")
            .build()
        ecosFormService.save(form)

        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val ref = EntityRef.create("", "form", "atts-test-form")
            val atts = recordsService.getAtts(
                ref,
                mapOf(
                    "formKey" to "formKey?str",
                    "id" to "?id"
                )
            )
            assertEquals("atts-key", atts["formKey"].asText())
            assertEquals("uiserv/form@atts-test-form", atts["id"].asText())
        }
    }

    @Test
    fun `mutate existing form with admin workspace should preserve data`() {
        // Create a form first
        ecosFormService.save(
            EcosFormDef.create()
                .withId("preserve-form")
                .withFormKey("initial")
                .build()
        )

        // Update via Records API with admin$workspace
        AuthContext.runAs("admin", listOf(AuthRole.ADMIN)) {
            val mutAtts = RecordAtts(EntityRef.create("", "form", "preserve-form"))
            mutAtts.setAtt(RecordConstants.ATT_WORKSPACE, "admin\$workspace")
            mutAtts.setAtt("formKey?str", "updated")

            val result = recordsService.mutate(mutAtts)
            assertEquals("preserve-form", result.getLocalId())
        }

        val found = ecosFormService.getFormById(IdInWs.create("preserve-form"))
        assertTrue(found.isPresent)
        assertEquals("updated", found.get().formKey)
    }
}
