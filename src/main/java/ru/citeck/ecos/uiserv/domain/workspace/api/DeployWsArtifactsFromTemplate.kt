package ru.citeck.ecos.uiserv.domain.workspace.api

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.utils.ZipUtils
import ru.citeck.ecos.uiserv.domain.workspace.service.WorkspaceUiService
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutor
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorReq
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorResp

@Component
class DeployWsArtifactsFromTemplate(
    private val workspaceUiService: WorkspaceUiService
) : EcosWebExecutor {

    companion object {
        private const val PATH = "/workspace/deploy-ws-artifacts-from-template"

        private const val HEADER_WORKSPACE = "workspace"
    }

    override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {
        val headers = request.getHeaders()
        val workspace = headers.get(HEADER_WORKSPACE) ?: ""
        if (workspace.isBlank()) {
            error("$HEADER_WORKSPACE header is empty")
        }
        val artifactsDir = ZipUtils.extractZip(request.getBodyReader().getInputStream())
        workspaceUiService.deployWsArtifactsFromTemplate(workspace, artifactsDir)
    }

    override fun getPath(): String {
        return PATH
    }

    override fun isReadOnly(): Boolean {
        return false
    }
}
