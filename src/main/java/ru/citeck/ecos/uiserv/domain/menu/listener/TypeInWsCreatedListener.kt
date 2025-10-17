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
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.events2.type.RecordDeletedEvent
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import java.util.*

@Component
class TypeInWsCreatedListener(
    private val eventsService: EventsService,
    private val menuService: MenuService,
    private val workspaceService: WorkspaceService
) {

    companion object {
        private val log = KotlinLogging.logger {}
        private const val AUTO_JOURNAL_MENU_ITEM_ID_PREFIX = "AUTO-J-"
    }

    @PostConstruct
    fun init() {
        val baseFilter = Predicates.and(
            Predicates.eq("typeDef.id", "type"),
            Predicates.notEmpty("record.workspace?str"),
        )
        eventsService.addListener(
            ListenerConfig.create<EventData>()
                .withEventType(RecordCreatedEvent.TYPE)
                .withTransactional(true)
                .withFilter(baseFilter)
                .withAction { handleEvent(it, EventType.CREATE) }
                .withDataClass(EventData::class.java)
                .build()
        )
        eventsService.addListener(
            ListenerConfig.create<EventData>()
                .withEventType(RecordDeletedEvent.TYPE)
                .withTransactional(true)
                .withFilter(baseFilter)
                .withAction { handleEvent(it, EventType.DELETE) }
                .withDataClass(EventData::class.java)
                .build()
        )
        eventsService.addListener(
            ListenerConfig.create<EventData>()
                .withEventType(RecordChangedEvent.TYPE)
                .withTransactional(true)
                .withFilter(
                    Predicates.and(
                        baseFilter,
                        Predicates.eq("diff._has.name?bool", true)
                    )
                )
                .withAction { handleEvent(it, EventType.UPDATE) }
                .withDataClass(EventData::class.java)
                .build()
        )
    }

    private fun handleEvent(event: EventData, eventType: EventType) {

        log.info { "Detected event $eventType for type in workspace: $event" }

        val menu = menuService.getMenuForCurrentUser(1, event.workspace)
        if (menu == null) {
            log.error {
                "Menu doesn't found for user ${AuthContext.getCurrentUser()} " +
                    "within workspace ${event.workspace}"
            }
            return
        }
        val leftMenuSrc = menu.subMenu["left"] ?: SubMenuDef()
        val leftMenuJson = DataValue.of(Json.mapper.toJson(leftMenuSrc))

        val label = if (MLText.isEmpty(event.name)) {
            MLText(workspaceService.convertToIdInWs(event.typeId).id)
        } else {
            event.name
        }
        val journalRef = "uiserv/journal@type$" + event.typeId
        when (eventType) {
            EventType.CREATE -> {
                leftMenuJson.add(
                    "\$..[?(@.id == \"sections\")].items",
                    DataValue.createObj()
                        .set(
                            "id",
                            AUTO_JOURNAL_MENU_ITEM_ID_PREFIX +
                                event.typeId + "-" +
                                System.currentTimeMillis().toString(Character.MAX_RADIX)
                        )
                        .set("label", label)
                        .set("type", "JOURNAL")
                        .set("icon", "ui/icon@i-leftmenu-types")
                        .set("config", DataValue.createObj().set("recordRef", journalRef))
                )
            }
            EventType.UPDATE -> {
                val sectionsWithItems = leftMenuJson["\$..[?(@.id == \"sections\")].items"]
                val autoItem = sectionsWithItems.firstNotNullOfOrNull { currentItems ->
                    currentItems.find {
                        it["type"].asText() == "JOURNAL" &&
                            it["/config/recordRef"].asText() == journalRef &&
                            it["id"].asText().startsWith(AUTO_JOURNAL_MENU_ITEM_ID_PREFIX)
                    }
                }
                if (autoItem == null) {
                    log.info {
                        "Auto item doesn't found for journalRef $journalRef " +
                            "in menu ${menu.id} in workspace ${menu.workspace}"
                    }
                    return
                } else {
                    autoItem["label"] = label
                }
            }
            // todo: delete event is not exists for types and this logic doesn't executed
            EventType.DELETE -> {
                val sectionsWithItems = leftMenuJson["\$..[?(@.id == \"sections\")].items"]
                var updated = false
                sectionsWithItems.forEach { currentItems ->
                    val autoItemIndex = currentItems.indexOfFirst {
                        it["type"].asText() == "JOURNAL" &&
                            it["/config/recordRef"].asText() == journalRef &&
                            it["id"].asText().startsWith(AUTO_JOURNAL_MENU_ITEM_ID_PREFIX)
                    }
                    if (autoItemIndex >= 0) {
                        log.info { "Remove auto item from menu: '${currentItems[autoItemIndex]["id"].asText()}'" }
                        currentItems.remove(autoItemIndex)
                        updated = true
                        return@forEach
                    }
                }
                if (!updated) {
                    log.debug {
                        "Auto item doesn't found for journalRef $journalRef " +
                            "in menu ${menu.id} in workspace ${menu.workspace}. Nothing to delete."
                    }
                    return
                }
            }
        }
        val newSubMenu = LinkedHashMap(menu.subMenu)
        newSubMenu["left"] = leftMenuJson.getAs(SubMenuDef::class.java) ?: leftMenuSrc

        val newMenu = menu.copy().withSubMenu(newSubMenu)
        if (newMenu.workspace.isBlank() || newMenu.workspace != event.workspace) {
            if (eventType == EventType.DELETE) {
                return
            }
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
        @AttName("record.name?json")
        val name: MLText
    )

    private enum class EventType {
        CREATE, DELETE, UPDATE
    }
}
