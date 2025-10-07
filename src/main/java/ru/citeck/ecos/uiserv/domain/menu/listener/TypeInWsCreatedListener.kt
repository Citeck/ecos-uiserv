package ru.citeck.ecos.uiserv.domain.menu.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.listener.ListenerConfig
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import java.util.UUID

@Component
class TypeInWsCreatedListener(
    private val eventsService: EventsService,
    private val menuService: MenuService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @PostConstruct
    fun init() {
        eventsService.addListener(
            ListenerConfig.create<EventData>()
                .withEventType(RecordCreatedEvent.TYPE)
                .withTransactional(true)
                .withFilter(
                    Predicates.and(
                        Predicates.eq("typeDef.id", "type"),
                        Predicates.notEmpty("record.workspace?str"),
                    )
                )
                .withAction { handleEvent(it) }
                .withDataClass(EventData::class.java)
                .build()
        )
    }

    private fun handleEvent(event: EventData) {
        log.info { "Detected type creation in workspace: $event" }

        val menu = menuService.getMenuForCurrentUser(1, event.workspace)
        if (menu == null) {
            log.error { "Menu doesn't found for user ${AuthContext.getCurrentUser()} within workspace ${event.workspace}" }
            return
        }
        val leftMenuSrc = menu.subMenu["left"] ?: SubMenuDef()
        val leftMenuJson = DataValue.of(Json.mapper.toJson(leftMenuSrc))

        leftMenuJson.add(
            "\$..[?(@.id == \"sections\")].items",
            DataValue.createObj()
                .set("id", UUID.randomUUID().toString())
                .set("label", event.name)
                .set("type", "JOURNAL")
                .set("icon", "ui/icon@i-leftmenu-types")
                .set(
                    "config",
                    DataValue.createObj()
                        .set("recordRef", "uiserv/journal@type$" + event.typeId)
                )
        )
        val newSubMenu = LinkedHashMap(menu.subMenu)
        newSubMenu["left"] = leftMenuJson.getAs(SubMenuDef::class.java) ?: leftMenuSrc

        val newMenu = menu.copy().withSubMenu(newSubMenu)
        if (newMenu.workspace.isBlank() || newMenu.workspace != event.workspace) {
            newMenu.withId(UUID.randomUUID().toString())
            newMenu.withWorkspace(event.workspace)
            if (newMenu.authorities.isEmpty()) {
                newMenu.withAuthorities(listOf("GROUP_EVERYONE"))
            }
        }

        menuService.save(newMenu.build())
    }

    data class EventData(
        @AttName("record?localId")
        val typeId: String,
        @AttName("record.workspace?str")
        val workspace: String,
        @AttName("record.name?json!record?localId")
        val name: MLText
    )
}
