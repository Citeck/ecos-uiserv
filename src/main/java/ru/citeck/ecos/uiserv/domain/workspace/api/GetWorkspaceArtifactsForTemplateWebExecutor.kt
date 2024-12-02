package ru.citeck.ecos.uiserv.domain.workspace.api

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.utils.ZipUtils
import ru.citeck.ecos.uiserv.domain.workspace.service.WorkspaceUiService
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutor
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorReq
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorResp

@Component
class GetWorkspaceArtifactsForTemplateWebExecutor(
    private val workspaceUiService: WorkspaceUiService
) : EcosWebExecutor {

    companion object {
        private const val PATH = "/workspace/get-ws-artifacts-for-template"
    }

    override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {

        val req = request.getBodyReader().readDto(GetWorkspaceArtifactsReq::class.java)

        response.getBodyWriter().getOutputStream().use {
            ZipUtils.writeZip(workspaceUiService.getWsArtifactsForTemplate(req.workspace), it)
        }
    }

    override fun getPath(): String {
        return PATH
    }

    override fun isReadOnly(): Boolean {
        return true
    }

    class GetWorkspaceArtifactsReq(
        val workspace: String
    )
}
