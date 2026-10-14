package ru.citeck.ecos.uiserv.app.common.utils

import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef

object WorkspaceRefUtils {

    /** Use the selectable default workspace to represent global artifacts in the UI. */
    @JvmStatic
    fun getWorkspaceRefOrDefault(workspace: String?): EntityRef {
        val ws = if (workspace.isNullOrBlank()) ModelUtils.DEFAULT_WORKSPACE_ID else workspace
        return ModelUtils.getWorkspaceRef(ws)
    }
}
