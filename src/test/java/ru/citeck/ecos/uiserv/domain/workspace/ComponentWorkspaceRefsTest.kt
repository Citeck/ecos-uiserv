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
    fun `dashboard journal lists round trip at every depth and preserve representation and non references`() {
        val config = ObjectData.create(
            """
            {
              "v2": {"widgets": [{"props": {"config": {"v2": {
                "journalId": "source:trips",
                "journalsListIds": ["uiserv/journal@source:trips", "type${'$'}source:purpose", "global", "uiserv/journal@foreign:trips", "", null, 42],
                "label": "uiserv/journal@source:trips",
                "script": "CURRENT_WS:trips"
              }}}}]},
              "nested": [[{"journalsListIds": ["source:other"]}]]
            }
            """.trimIndent()
        )
        val before = config.deepCopy()
        val exported = ComponentWorkspaceRefs.rewrite(config, export)
        val imported = ComponentWorkspaceRefs.rewrite(exported, import)

        assertThat(exported.getData().asJson().at("/v2/widgets/0/props/config/v2/journalsListIds/0").asText())
            .isEqualTo("uiserv/journal@CURRENT_WS:trips")
        assertThat(imported.getData().asJson().at("/v2/widgets/0/props/config/v2/journalsListIds"))
            .isEqualTo(ObjectData.create("""{"items":["uiserv/journal@target:trips","type${'$'}target:purpose","global","uiserv/journal@foreign:trips","",null,42]}""").getData().asJson().get("items"))
        assertThat(imported.getData().asJson().at("/nested/0/0/journalsListIds/0").asText()).isEqualTo("target:other")
        assertThat(imported.getData().asJson().at("/v2/widgets/0/props/config/v2/label").asText())
            .isEqualTo("uiserv/journal@source:trips")
        assertThat(imported.getData().asJson().at("/v2/widgets/0/props/config/v2/script").asText()).isEqualTo("CURRENT_WS:trips")
        assertThat(config).isEqualTo(before)
    }

    @Test
    fun `empty non textual and unrelated properties are unchanged`() {
        val config = ObjectData.create("""{"journalId":"","items":[{"journalId":null},{"journalId":42}],"label":"source:trips"}""")
        assertThat(ComponentWorkspaceRefs.rewrite(config, export)).isEqualTo(config)
    }
}
