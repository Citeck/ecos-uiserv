package ru.citeck.ecos.uiserv.domain.menu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import ru.citeck.ecos.apps.app.domain.handler.ArtifactDeployMeta
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.listener.ListenerConfig
import ru.citeck.ecos.events2.listener.ListenerHandle
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.listener.TypeInWsCreatedListener
import ru.citeck.ecos.uiserv.domain.menu.service.testutils.MenuTestBase
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.util.Base64

class WorkspaceMenuImportTest : MenuTestBase() {

    @Test
    fun `import replaces only the automatic template copy and preserves other type entries`() {
        deployMenuArtifacts()
        val generated = menu("generated", listOf(autoItem("trip", "new"), autoItem("existing", "old")))
        val imported = menu("imported", listOf(autoItem("trip", "source"), customItem("board")))

        AuthContext.runAsSystem {
            menuService.save(generated)
            menuService.upload(imported, "target")
            // The old automatic copy can remain in the artifact registry and be delivered again.
            val replay = ArtifactDeployMeta.create().withSourceType("USER").build()
            ArtifactDeployMeta.doWithMeta(replay) { menuService.upload(generated, "target") }
        }

        val menus = menuService.findAllForWorkspace("target")
        assertThat(menus.map { it.id }).containsExactly("imported")
        assertThat(sectionItems(menus.single()).map { it.id })
            .contains("AUTO-J-target:trip-source", "AUTO-J-target:existing-old", "board")
            .doesNotContain("AUTO-J-target:trip-new")
    }

    @Test
    fun `import preserves customized group and foreign workspace menus`() {
        deployMenuArtifacts()
        val custom = menu("custom", listOf(autoItem("trip", "old"), customItem("custom-board")))
        val group = menu("group", listOf(autoItem("trip", "old"))).copy()
            .withAuthorities(listOf("GROUP_accounting")).build()
        val foreign = menu("foreign", listOf(autoItem("trip", "old"))).copy().withWorkspace("foreign").build()
        val imported = menu("imported", listOf(autoItem("trip", "source"), customItem("board")))

        AuthContext.runAsSystem {
            menuService.save(custom)
            menuService.save(group)
            menuService.save(foreign)
            menuService.upload(imported, "target")
            menuService.upload(imported, "target")
        }

        assertThat(menuService.findAllForWorkspace("target").map { it.id })
            .containsExactlyInAnyOrder("custom", "group", "imported")
        assertThat(menuService.getMenu(IdInWs.create("foreign", "foreign"))).isPresent
        assertThat(sectionItems(menuService.getMenu(IdInWs.create("target", "custom")).orElseThrow()).map { it.id })
            .contains("custom-board")
    }

    @Test
    fun `type events around app import leave one menu and one item per type and rename it`() {
        deployMenuArtifacts()
        val listeners = typeListeners()

        AuthContext.runAsSystem {
            listeners.getValue(RecordCreatedEvent.TYPE).action.accept(typeEvent("Before"))
            assertThat(menuService.findAllForWorkspace("target")).hasSize(1)

            menuService.upload(menu("imported", listOf(autoItem("trip", "source"), customItem("board"))), "target")
            listeners.getValue(RecordCreatedEvent.TYPE).action.accept(typeEvent("Imported"))
            listeners.getValue(RecordCreatedEvent.TYPE).action.accept(typeEvent("Imported"))
            listeners.getValue(RecordChangedEvent.TYPE).action.accept(typeEvent("Renamed"))
        }

        val menus = menuService.findAllForWorkspace("target")
        assertThat(menus.map { it.id }).containsExactly("imported")
        val items = sectionItems(menus.single())
        val automatic = items.filter { it.id.startsWith("AUTO-J-target:trip-") }
        assertThat(automatic).hasSize(1)
        assertThat(automatic.single().label).isEqualTo(MLText("Renamed"))
        assertThat(items.map { it.id }).contains("board")
    }

    @Test
    fun `app import keeps its explicit menu id when an existing menu is customized`() {
        deployMenuArtifacts()
        val existing = menu("customized", listOf(customItem("board")))
        val imported = menu("app-menu", listOf(autoItem("trip", "source")))
        val meta = ArtifactDeployMeta.create().withSourceType("ECOS_APP").build()

        AuthContext.runAsSystem {
            menuService.save(existing)
            ArtifactDeployMeta.doWithMeta(meta) { menuService.upload(imported, "target") }
        }

        assertThat(menuService.getMenu(IdInWs.create("target", "app-menu"))).isPresent
        assertThat(sectionItems(menuService.getMenu(IdInWs.create("target", "customized")).orElseThrow()).map { it.id })
            .contains("board")
    }

    @Test
    fun `standalone menu upload also replaces automatic copy and remains idempotent`() {
        deployMenuArtifacts()
        val exported = menu("imported", listOf(autoItem("trip", "source"), customItem("board")))
            .copy().withWorkspace("").build()
        val content = "data:application/json;base64," + Base64.getEncoder().encodeToString(Json.mapper.toBytes(exported))

        AuthContext.runAsSystem {
            menuService.save(menu("generated", listOf(autoItem("trip", "old"))))
            repeat(2) {
                records.mutate(
                    "menu@",
                    linkedMapOf(
                        "_workspace" to "emodel/workspace@target",
                        "_content" to listOf(mapOf("url" to content))
                    )
                )
            }
        }

        assertThat(menuService.findAllForWorkspace("target").map { it.id }).containsExactly("imported")
        assertThat(sectionItems(menuService.getMenu(IdInWs.create("target", "imported")).orElseThrow()).map { it.id })
            .contains("board", "AUTO-J-target:trip-source").doesNotContain("AUTO-J-target:trip-old")
    }

    private fun typeListeners(): Map<String, ListenerConfig<TypeInWsCreatedListener.TypeEventData>> {
        val listeners = mutableMapOf<String, ListenerConfig<TypeInWsCreatedListener.TypeEventData>>()
        val events = Mockito.mock(EventsService::class.java)
        Mockito.doAnswer { invocation ->
            val config = invocation.getArgument<ListenerConfig<TypeInWsCreatedListener.TypeEventData>>(0)
            listeners[config.eventType] = config
            Mockito.mock(ListenerHandle::class.java)
        }.`when`(events).addListener(any<ListenerConfig<*>>())

        TypeInWsCreatedListener(
            events,
            menuService,
            workspaceService,
            Mockito.mock(JournalService::class.java),
            Mockito.mock(EcosTypeService::class.java),
            Mockito.mock(EcosTypesRegistry::class.java)
        ).init()
        return listeners
    }

    private fun typeEvent(name: String): TypeInWsCreatedListener.TypeEventData {
        val journal = EntityRef.valueOf("uiserv/journal@type${'$'}target:trip")
        return TypeInWsCreatedListener.TypeEventData(
            "target:trip",
            journal,
            journal,
            "target",
            MLText(name),
            "",
            emptySet()
        )
    }

    private fun menu(id: String, items: List<MenuItemDef>): MenuDto {
        val template = menuService.getMenu(IdInWs.create("default-ws-menu")).orElseThrow()
        val menu = template.copy().withId(id).withWorkspace("target")
            .withAuthorities(listOf(AuthGroup.EVERYONE)).build()
        val left = requireNotNull(menu.subMenu["left"])
        left.items = left.items.map { section ->
            if (section.id == "sections") {
                section.copy().withItems(section.items + items).build()
            } else {
                section
            }
        }
        return menu
    }

    private fun sectionItems(menu: MenuDto): List<MenuItemDef> {
        return requireNotNull(menu.subMenu["left"]).items.single { it.id == "sections" }.items
    }

    private fun autoItem(typeId: String, suffix: String): MenuItemDef {
        return MenuItemDef.create().withId("AUTO-J-target:$typeId-$suffix").withType("JOURNAL")
            .withLabel(MLText(typeId))
            .withConfig(ObjectData.create().set("recordRef", "uiserv/journal@type${'$'}target:$typeId")).build()
    }

    private fun customItem(id: String): MenuItemDef {
        return MenuItemDef.create().withId(id).withType("KANBAN")
            .withConfig(ObjectData.create().set("recordRef", "uiserv/board@target:$id")).build()
    }
}
