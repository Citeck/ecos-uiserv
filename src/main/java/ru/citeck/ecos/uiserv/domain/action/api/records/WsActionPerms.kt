package ru.citeck.ecos.uiserv.domain.action.api.records

import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records3.record.atts.value.AttValue

/**
 * Permissions of an action in workspace: everyone may read it,
 * admins and managers of the workspace may change it.
 */
class WsActionPerms(
    private val workspace: String,
    private val workspaceService: WorkspaceService
) : AttValue {

    override fun has(name: String): Boolean {
        return if (name.equals("write", ignoreCase = true)) {
            AuthContext.isRunAsSystemOrAdmin() ||
                workspaceService.isUserManagerOf(AuthContext.getCurrentUser(), workspace)
        } else {
            name.equals("read", ignoreCase = true)
        }
    }
}
