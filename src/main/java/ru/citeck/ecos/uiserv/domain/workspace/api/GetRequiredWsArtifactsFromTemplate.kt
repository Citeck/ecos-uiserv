package ru.citeck.ecos.uiserv.domain.workspace.api

import org.springframework.stereotype.Component
import ru.citeck.ecos.uiserv.domain.workspace.service.WorkspaceUiService
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutor
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorReq
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorResp

@Component
class GetRequiredWsArtifactsFromTemplate : EcosWebExecutor {

    companion object {
        private const val PATH = "/workspace/get-required-ws-artifacts-from-template"
    }

    override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {
        response.getBodyWriter().writeDto(
            GetRequiredWsArtifactsResp(WorkspaceUiService.REQUIRED_WS_ARTIFACTS_FROM_TEMPLATE)
        )
    }

    override fun getPath(): String {
        return PATH
    }

    override fun isReadOnly(): Boolean {
        return true
    }

    class GetRequiredWsArtifactsResp(
        val paths: List<String>
    )
}
