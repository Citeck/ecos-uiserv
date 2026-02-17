package ru.citeck.ecos.uiserv.domain.form.api.records

import com.fasterxml.jackson.annotation.JsonProperty
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef

class EcosFormMutRecord(
    other: EcosFormDef,
    private val workspaceService: WorkspaceService
) : EcosFormDef.Builder(other) {

    constructor(workspaceService: WorkspaceService) : this(EcosFormDef.EMPTY, workspaceService)

    val originalId = other.id
    val originalWorkspace = other.workspace

    /**
     * The raw _workspace value from the mutation request.
     * Stored separately because _self content-data expansion may overwrite
     * the `workspace` field after this method is called.
     */
    var ctxWorkspace: String? = null
        private set

    @JsonProperty(RecordConstants.ATT_WORKSPACE)
    fun withCtxWorkspace(workspace: String) {
        ctxWorkspace = workspace
        if (originalId.isNotBlank() && originalId != id) {
            withWorkspace(workspace)
        } else {
            withWorkspace(workspaceService.getUpdatedWsInMutation(this.workspace, workspace))
        }
    }

    fun withModuleId(value: String?) {
        withId(value)
    }
}
