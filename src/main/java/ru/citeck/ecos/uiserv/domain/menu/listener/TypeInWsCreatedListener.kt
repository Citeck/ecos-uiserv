package ru.citeck.ecos.uiserv.domain.menu.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.listener.ListenerConfig
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.events2.type.RecordDeletedEvent
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.api.records.JournalRecordsDao
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.uiserv.domain.journal.service.JournalServiceImpl
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.util.*
import kotlin.collections.set

@Component
class TypeInWsCreatedListener(
    private val eventsService: EventsService,
    private val menuService: MenuService,
    private val workspaceService: WorkspaceService,
    private val journalService: JournalService,
    private val ecosTypeService: EcosTypeService,
    private val ecosTypesRegistry: EcosTypesRegistry
) {

    companion object {
        private val log = KotlinLogging.logger {}
        private const val AUTO_JOURNAL_MENU_ITEM_ID_PREFIX = "AUTO-J-"

        private const val JOURNAL_REF_SRC_ID = "uiserv/journal"
        private const val SECTION_ITEMS_JSON_PATH = "$..[?(@.id == \"sections\")].items"
    }

    @PostConstruct
    fun init() {
        val baseFilter = Predicates.and(
            Predicates.eq("typeDef.id", "type"),
            Predicates.notEmpty("record.workspace?str"),
        )
        eventsService.addListener(
            ListenerConfig.create<TypeEventData>()
                .withEventType(RecordCreatedEvent.TYPE)
                .withTransactional(true)
                .withFilter(baseFilter)
                .withAction { handleEvent(it, TypeEventType.CREATE) }
                .withDataClass(TypeEventData::class.java)
                .build()
        )
        eventsService.addListener(
            ListenerConfig.create<TypeEventData>()
                .withEventType(RecordDeletedEvent.TYPE)
                .withTransactional(true)
                .withFilter(baseFilter)
                .withAction { handleEvent(it, TypeEventType.DELETE) }
                .withDataClass(TypeEventData::class.java)
                .build()
        )
        eventsService.addListener(
            ListenerConfig.create<TypeEventData>()
                .withEventType(RecordChangedEvent.TYPE)
                .withTransactional(true)
                .withFilter(
                    Predicates.and(
                        baseFilter,
                        Predicates.or(
                            Predicates.eq("diff._has.name?bool", true),
                            Predicates.eq("diff._has.journalRef?bool", true)
                        )
                    )
                )
                .withAction { handleEvent(it, TypeEventType.UPDATE) }
                .withDataClass(TypeEventData::class.java)
                .build()
        )
        journalService.onJournalChanged { before, after -> handleJournalChanged(before, after) }
    }

    private fun handleJournalChanged(before: JournalDef?, after: JournalDef?) {
        after ?: return
        if (before?.name == after.name || MLText.isEmpty(after.name)) {
            return
        }
        val typeRef = getTypeRefForJournal(after)
        val idInWs = workspaceService.convertToIdInWs(typeRef.getLocalId())
        if (idInWs.workspace.isEmpty()) {
            return
        }
        val journalLocalId = workspaceService.addWsPrefixToId(after.id, after.workspace)
        log.info { "Detected journal name changed event for journal '$journalLocalId' with type $typeRef" }
        doWithMenuForCurrentUser(
            idInWs.workspace,
            journalLocalId,
            typeRef.getLocalId(),
            ecosTypesRegistry.getTypeInfo(typeRef)?.name ?: MLText()
        ) { menuCtx ->
            menuCtx.updateMenuItem { it.set("label", after.name) }
        }
    }

    private fun getTypeRefForJournal(journalDef: JournalDef): EntityRef {
        if (EntityRef.isNotEmpty(journalDef.typeRef)) {
            return journalDef.typeRef
        }
        val localId = workspaceService.addWsPrefixToId(journalDef.id, journalDef.workspace)
        val journalRef = EntityRef.create(AppName.UISERV, JournalRecordsDao.ID, localId)
        return ecosTypeService.getTypeRefByJournal(journalRef)
    }

    private fun handleEvent(event: TypeEventData, eventType: TypeEventType) {

        log.info { "Detected event $eventType for type in workspace: $event" }

        doWithMenuForCurrentUser(
            event.workspace,
            event.journalRefAfter.getLocalId(),
            event.typeId,
            event.typeName
        ) { menuCtx ->

            when (eventType) {
                TypeEventType.CREATE -> {
                    if (event.journalRefAfter.isNotEmpty()) {
                        menuCtx.addMenuItem()
                    }
                }
                TypeEventType.UPDATE -> {
                    if (event.journalRefBefore.isNotEmpty() && event.journalRefAfter.isEmpty()) {
                        menuCtx.deleteMenuItem()
                    } else if (event.journalRefBefore.isEmpty() && event.journalRefAfter.isNotEmpty()) {
                        menuCtx.addMenuItem()
                    } else {
                        menuCtx.updateLabel()
                    }
                }
                TypeEventType.DELETE -> {
                    menuCtx.deleteMenuItem()
                }
            }
        }
    }

    private fun doWithMenuForCurrentUser(
        workspace: String,
        journalId: String,
        typeId: String,
        typeName: MLText,
        action: (MenuUpdateContext) -> Unit
    ) {

        val menu = menuService.getMenuForCurrentUser(1, workspace)
        if (menu == null) {
            log.error {
                "Menu doesn't found for user ${AuthContext.getCurrentUser()} " +
                    "within workspace $workspace"
            }
            return
        }

        val leftMenuSrc = menu.subMenu["left"] ?: SubMenuDef()
        val leftMenuJson = DataValue.of(Json.mapper.toJson(leftMenuSrc))

        val context = MenuUpdateContext(menu.id, workspace, leftMenuJson, journalId, typeId, typeName)
        action(context)

        val newSubMenu = LinkedHashMap(menu.subMenu)
        newSubMenu["left"] = leftMenuJson.getAs(SubMenuDef::class.java) ?: leftMenuSrc

        val newMenu = menu.copy().withSubMenu(newSubMenu)
        if (newMenu.workspace.isBlank() || newMenu.workspace != workspace) {
            if (!context.autoItemWasChanged) {
                return
            }
            newMenu.withId(UUID.randomUUID().toString())
            newMenu.withWorkspace(workspace)
            if (newMenu.authorities.isEmpty()) {
                newMenu.withAuthorities(listOf(AuthGroup.EVERYONE))
            }
        }
        menuService.save(newMenu.build())
    }

    data class TypeEventData(
        @param:AttName("record?localId")
        val typeId: String,
        @param:AttName("before.journalRef?id!")
        val journalRefBefore: EntityRef,
        @param:AttName("record.journalRef?id!")
        val journalRefAfter: EntityRef,
        @param:AttName("record.workspace?str")
        val workspace: String,
        @param:AttName("record.name?json")
        val typeName: MLText
    )

    private inner class MenuUpdateContext(
        private val menuId: String,
        private val workspace: String,
        private val leftMenuJson: DataValue,
        private val journalId: String,
        private val typeId: String,
        private val typeName: MLText
    ) {
        // true if items were added or updated (not on delete)
        var autoItemWasChanged: Boolean = false
        val autoJournalRef = "$JOURNAL_REF_SRC_ID@type$$typeId"

        val label by lazy { evalMenuItemLabel() }

        fun addMenuItem() {
            val item = DataValue.createObj()
                .set(
                    "id",
                    AUTO_JOURNAL_MENU_ITEM_ID_PREFIX +
                        typeId + "-" +
                        System.currentTimeMillis().toString(Character.MAX_RADIX)
                )
                .set("label", label)
                .set("type", "JOURNAL")
                .set("icon", "ui/icon@i-leftmenu-types")
                .set("config", DataValue.createObj().set("recordRef", autoJournalRef))

            leftMenuJson.add(SECTION_ITEMS_JSON_PATH, item)
            autoItemWasChanged = true
        }

        fun deleteMenuItem() {
            val sectionsWithItems = leftMenuJson[SECTION_ITEMS_JSON_PATH]
            var updated = false
            sectionsWithItems.forEach { currentItems ->
                val autoItemIndex = currentItems.indexOfFirst {
                    isElementAutoItemForCurrentJournal(it)
                }
                if (autoItemIndex >= 0) {
                    log.info {
                        "Remove auto item. Menu: '$menuId'. " +
                            "Item: '${currentItems[autoItemIndex]["id"].asText()}'"
                    }
                    currentItems.remove(autoItemIndex)
                    updated = true
                    return@forEach
                } else {
                    logAutoItemNotFound()
                }
            }
            if (!updated) {
                logAutoItemNotFound()
            }
        }

        fun updateMenuItem(action: (DataValue) -> DataValue) {
            val sectionsWithItems = leftMenuJson[SECTION_ITEMS_JSON_PATH]
            val autoItem = sectionsWithItems.firstNotNullOfOrNull { currentItems ->
                currentItems.find { isElementAutoItemForCurrentJournal(it) }
            }
            if (autoItem == null) {
                logAutoItemNotFound()
            } else {
                action(autoItem)
                autoItemWasChanged = true
            }
        }

        fun updateLabel() {
            updateMenuItem { it.set("label", label) }
        }

        private fun evalMenuItemLabel(): MLText {
            if (journalId.isNotEmpty() &&
                journalId != JournalServiceImpl.DEFAULT_AUTO_JOURNAL_FOR_TYPE &&
                !journalId.startsWith("type$")
            ) {
                val journal = journalService.getJournalById(workspaceService.convertToIdInWs(journalId))
                val journalName = journal?.journalDef?.name
                if (journalName != null && !MLText.isEmpty(journalName)) {
                    return journalName
                }
            }
            return if (MLText.isEmpty(typeName)) {
                MLText(workspaceService.convertToIdInWs(typeId).id)
            } else {
                typeName
            }
        }

        private fun logAutoItemNotFound() {
            log.debug {
                "Auto item doesn't found for journalRef $autoJournalRef " +
                    "in menu $menuId in workspace $workspace"
            }
        }

        private fun isElementAutoItemForCurrentJournal(element: DataValue): Boolean {
            return element["type"].asText() == "JOURNAL" &&
                element["/config/recordRef"].asText() == autoJournalRef &&
                element["id"].asText().startsWith(AUTO_JOURNAL_MENU_ITEM_ID_PREFIX)
        }
    }

    private enum class TypeEventType {
        CREATE,
        DELETE,
        UPDATE
    }
}
