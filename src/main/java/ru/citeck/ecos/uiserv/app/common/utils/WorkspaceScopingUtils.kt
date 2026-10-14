package ru.citeck.ecos.uiserv.app.common.utils

import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates

object WorkspaceScopingUtils {

    @JvmStatic
    fun applyScoping(
        predicate: Predicate,
        workspaces: List<String>,
        skipWorkspaceScoping: Boolean,
        workspaceService: WorkspaceService
    ): Predicate {
        if (skipWorkspaceScoping && AuthContext.isRunAsSystemOrAdmin()) {
            return predicate
        }
        return Predicates.and(
            predicate,
            workspaceService.buildAvailableWorkspacesPredicate(AuthContext.getCurrentRunAsAuth(), workspaces)
        )
    }

    /** Artifacts visible through an admin workspace filter must also be readable by ID. */
    @JvmStatic
    fun isArtifactReadable(workspaceService: WorkspaceService, workspace: String?): Boolean {
        return AuthContext.isRunAsSystemOrAdmin() ||
            workspaceService.isWorkspaceWithGlobalEntities(workspace) ||
            workspaceService.isUserMemberOf(AuthContext.getCurrentUser(), workspace ?: "")
    }
}
