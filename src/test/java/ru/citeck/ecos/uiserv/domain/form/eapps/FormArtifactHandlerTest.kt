package ru.citeck.ecos.uiserv.domain.form.eapps

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.citeck.ecos.apps.app.domain.handler.ArtifactDeployMeta
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.webapp.api.entity.EntityRef

class FormArtifactHandlerTest {

    private val ws = "custom"
    private val wsPrefix = "custom-sys-id:"

    private val formService = mock<EcosFormService>()
    private val handler = FormArtifactHandler(formService, fakeWorkspaceService())

    @Test
    fun `deploy promotes co-deployed typeRef and sets workspace`() {
        val artifact = EcosFormDef.create()
            .withId("my-form")
            .withTypeRef(EntityRef.valueOf("emodel/type@my-type"))
            .build()

        val saved = deploy(artifact, listOf(EntityRef.valueOf("emodel/type@my-type")))

        assertThat(saved.typeRef.toString()).isEqualTo("emodel/type@${wsPrefix}my-type")
        assertThat(saved.workspace).isEqualTo(ws)
    }

    @Test
    fun `deploy rebinds CURRENT_WS typeRef`() {
        val artifact = EcosFormDef.create()
            .withId("my-form")
            .withTypeRef(EntityRef.valueOf("emodel/type@CURRENT_WS:my-type"))
            .build()

        val saved = deploy(artifact, emptyList())

        assertThat(saved.typeRef.toString()).isEqualTo("emodel/type@${wsPrefix}my-type")
    }

    @Test
    fun `deploy keeps non co-deployed typeRef global`() {
        val artifact = EcosFormDef.create()
            .withId("my-form")
            .withTypeRef(EntityRef.valueOf("emodel/type@global-type"))
            .build()

        val saved = deploy(artifact, listOf(EntityRef.valueOf("emodel/type@my-type")))

        assertThat(saved.typeRef.toString()).isEqualTo("emodel/type@global-type")
    }

    private fun deploy(artifact: EcosFormDef, coDeployed: List<EntityRef>): EcosFormDef {
        val meta = ArtifactDeployMeta.create().withCoDeployedArtifacts(coDeployed).build()
        ArtifactDeployMeta.doWithMeta(meta) {
            handler.deployArtifact(artifact, ws)
        }
        val captor = argumentCaptor<EcosFormDef>()
        verify(formService).save(captor.capture())
        return captor.firstValue
    }

    private fun fakeWorkspaceService(): WorkspaceService {
        val service = mock<WorkspaceService>()
        whenever(service.replaceCurrentWsPlaceholderToWsPrefix(any(), any())).doAnswer { inv ->
            val id = inv.getArgument<String>(0)
            val workspace = inv.getArgument<String>(1)
            val ph = "CURRENT_WS${IdInWs.WS_DELIM}"
            if (!id.startsWith(ph) || workspace != ws) id else "$wsPrefix${id.substring(ph.length)}"
        }
        whenever(service.addWsPrefixToId(any(), any())).doAnswer { inv ->
            val localId = inv.getArgument<String>(0)
            val workspace = inv.getArgument<String>(1)
            if (workspace != ws || localId.startsWith(wsPrefix)) localId else "$wsPrefix$localId"
        }
        return service
    }
}
