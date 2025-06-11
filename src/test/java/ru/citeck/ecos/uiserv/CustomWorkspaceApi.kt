package ru.citeck.ecos.uiserv

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.workspace.api.WorkspaceApi
import ru.citeck.ecos.model.lib.workspace.api.WsMembershipType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class CustomWorkspaceApi : WorkspaceApi {

    private val workspacesByUser = ConcurrentHashMap<String, MutableSet<String>>()
    private val managersByWorkspace = ConcurrentHashMap<String, MutableSet<String>>()

    fun addMember(workspace: String, user: String) {
        workspacesByUser.computeIfAbsent(user) { Collections.newSetFromMap(ConcurrentHashMap()) }.add(workspace)
    }

    fun addManager(workspace: String, user: String) {
        addMember(workspace, user)
        managersByWorkspace.computeIfAbsent(workspace) { Collections.newSetFromMap(ConcurrentHashMap()) }.add(user)
    }

    fun clear() {
        workspacesByUser.clear()
        managersByWorkspace.clear()
    }

    override fun getUserWorkspaces(user: String, membershipType: WsMembershipType): Set<String> {
        return workspacesByUser[user] ?: emptySet()
    }

    override fun isUserManagerOf(user: String, workspace: String): Boolean {
        return managersByWorkspace[workspace]?.contains(user) ?: false
    }
}
