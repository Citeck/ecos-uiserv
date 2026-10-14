package ru.citeck.ecos.uiserv.domain.workspace

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto
import ru.citeck.ecos.uiserv.domain.dashdoard.service.utils.DashboardWorkspaceRefs
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.uiserv.domain.form.service.utils.FormWorkspaceRefs
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Function

class ComponentWorkspaceRefsTest {

    private val export = Function<EntityRef, EntityRef> { ref ->
        ref.withLocalId(ref.getLocalId().replace("source:", "CURRENT_WS:"))
    }
    private val import = Function<EntityRef, EntityRef> { ref ->
        ref.withLocalId(ref.getLocalId().replace("CURRENT_WS:", "target:"))
    }

    @Test
    fun `form component journals round trip through nested containers without changing text`() {
        val definition = ObjectData.create("""{"components":[{"components":[{"type":"selectJournal","journalId":"type${'$'}source:purpose","source":{"journal":{"journalId":"uiserv/journal@source:custom"}},"label":"source:purpose","script":"CURRENT_WS:purpose"}]}]}""")
        val form = EcosFormDef.create().withDefinition(definition).build()
        val exported = FormWorkspaceRefs.rewrite(form, export)
        val imported = FormWorkspaceRefs.rewrite(exported, import)
        assertThat(exported.definition.toString()).contains("type\$CURRENT_WS:purpose", "uiserv/journal@CURRENT_WS:custom")
        assertThat(imported.definition.toString()).contains("type\$target:purpose", "uiserv/journal@target:custom")
        assertThat(imported.definition.toString()).contains("source:purpose", "CURRENT_WS:purpose")
        assertThat(form.definition).isEqualTo(definition)
        assertThat(form.definition.toString()).doesNotContain("target:")
    }

    @Test
    fun `dashboard journal config supports both versions and preserves global and foreign ids`() {
        val config = ObjectData.create("""{"v2":{"widgets":[{"props":{"config":{"v2":{"journalId":"source:trips"}}}}]},"widgets":[{"config":{"journalId":"global"}},{"config":{"journalId":"foreign:trips"}}]}""")
        val dashboard = DashboardDto.create().withConfig(config).build()
        val imported = DashboardWorkspaceRefs.rewrite(DashboardWorkspaceRefs.rewrite(dashboard, export), import)
        assertThat(imported.config.toString()).contains("target:trips", "global", "foreign:trips")
        assertThat(dashboard.config).isEqualTo(config)
    }

    @Test
    fun `empty non textual and unrelated properties are unchanged`() {
        val config = ObjectData.create("""{"journalId":"","items":[{"journalId":null},{"journalId":42}],"label":"source:trips"}""")
        assertThat(ComponentWorkspaceRefs.rewrite(config, export)).isEqualTo(config)
    }
}
