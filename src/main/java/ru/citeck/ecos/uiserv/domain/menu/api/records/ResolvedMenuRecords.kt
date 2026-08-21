package ru.citeck.ecos.uiserv.domain.menu.api.records

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@Component
class ResolvedMenuRecords(
    private val menuRecords: MenuRecords,
    private val ecosTypeService: EcosTypeService,
    private val workspaceService: WorkspaceService
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordsQueryDao {

    companion object {
        const val ID = "rmenu"

        private val log = KotlinLogging.logger {}

        private const val ITEM_TYPE_LINK_CREATE_CASE = "LINK-CREATE-CASE"
        private const val ITEM_TYPE_INCLUDE_MENU = "INCLUDE_MENU"
        private const val ITEM_TYPE_CREATE_IN_SECTION = "CREATE_IN_SECTION"

        private const val ROLE_WS_MANAGER = AuthRole.PREFIX + "WS_MANAGER"
        private const val ROLE_WS_USER = AuthRole.PREFIX + "WS_USER"

        /**
         * Menus which are being resolved by the current thread right now.
         */
        private val resolvingMenus = ThreadLocal.withInitial { LinkedHashSet<EntityRef>() }

        private inline fun <T> doWithMenuInResolving(menuRef: EntityRef, action: () -> T): T {
            if (menuRef.isEmpty()) {
                return action.invoke()
            }
            val menus = resolvingMenus.get()
            if (!menus.add(menuRef)) {
                // menu was already registered by the outer call
                return action.invoke()
            }
            try {
                return action.invoke()
            } finally {
                menus.remove(menuRef)
                if (menus.isEmpty()) {
                    resolvingMenus.remove()
                }
            }
        }
    }

    override fun getId() = ID

    override fun getRecordAtts(recordId: String): Any? {
        val atts = menuRecords.getRecordAtts(recordId)
        return ResolvedMenu(atts, atts.model.workspace)
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        val records = menuRecords.queryRecords(recsQuery)
        val result = RecsQueryRes<Any>()

        val workspace = recsQuery.query["workspace"].asText()

        result.setRecords(
            records.getRecords().map {
                if (it is MenuRecords.MenuRecord) {
                    ResolvedMenu(it, workspace)
                } else {
                    it
                }
            }
        )
        result.setTotalCount(records.getTotalCount())
        result.setHasMore(records.getHasMore())

        return result
    }

    inner class ResolvedMenu(
        @AttName("...")
        val menu: MenuRecords.MenuRecord,
        val workspace: String
    ) {

        fun getSubMenu(): SubMenus {
            return SubMenus(workspace, menu.getRef(), menu.model.subMenu)
        }
    }

    inner class SubMenus(workspace: String, menuRef: EntityRef, originalDef: Map<String, SubMenuDef>) {

        private val subMenus: Map<String, SubMenuDef>

        private val currentWorkspace = workspace.ifBlank { ModelUtils.DEFAULT_WORKSPACE_ID }
        private val currentUser: String
        private val currentUserWithAuthorities: Set<String>

        private var isUserWsManager: Boolean? = null
        private var isUserWsMember: Boolean? = null

        private fun isUserWsManager(): Boolean {
            isUserWsManager?.let { return it }
            return workspaceService.isUserManagerOf(currentUser, currentWorkspace).also {
                isUserWsManager = it
            }
        }

        private fun isUserWsMember(): Boolean {
            isUserWsMember?.let { return it }
            if (isUserWsManager == true) {
                return true
            }
            return workspaceService.isUserMemberOf(currentUser, currentWorkspace).also {
                isUserWsMember = it
            }
        }

        init {
            val currentAuth = AuthContext.getCurrentFullAuth()
            currentUser = currentAuth.getUser()
            currentUserWithAuthorities = LinkedHashSet(currentAuth.getAuthorities().size + 1)
            currentUserWithAuthorities.add(currentUser.lowercase())
            currentAuth.getAuthorities().forEach {
                currentUserWithAuthorities.add(it.lowercase())
            }
            subMenus = doWithMenuInResolving(normalizeMenuRef(menuRef)) {
                processMenuItems(originalDef)
            }
        }

        fun getUser(): SubMenuDef {
            return subMenus["user"] ?: SubMenuDef()
        }

        fun getLeft(): SubMenuDef {
            return subMenus["left"] ?: SubMenuDef()
        }

        fun getCreate(): SubMenuDef {
            return subMenus["create"] ?: SubMenuDef()
        }

        private fun findMenuItemById(items: List<MenuItemDef>?, id: String): MenuItemDef? {
            items?.forEach { item ->
                if (item.id == id) {
                    return item
                }
                val result = findMenuItemById(item.items, id)
                if (result != null) {
                    return result
                }
            }
            return null
        }

        private fun evalCreateVariants(section: MenuItemDef?): List<MenuItemDef> {

            section ?: return emptyList()

            val visitedTypes = HashSet<EntityRef>()

            val variants = mutableMapOf<String, MutableList<CreateVariantDef>>()
            val sectionNameById = mutableMapOf<String, MLText>()
            sectionNameById[section.id] = section.label

            section.items.forEach { item0 ->

                if (item0.type == "SECTION") {

                    sectionNameById[item0.id] = item0.label
                    val variantsRes = variants.computeIfAbsent(item0.id) { ArrayList() }
                    extractCreateVariantsFromItem(item0, variantsRes, visitedTypes)
                } else if (item0.type == "JOURNAL") {

                    val variantsRes = variants.computeIfAbsent(section.id) { ArrayList() }
                    extractCreateVariantsFromItem(item0, variantsRes, visitedTypes)
                }
            }

            val result = ArrayList<MenuItemDef>()

            variants.forEach { (id, variants) ->

                val sectionItemDef = MenuItemDef.create()
                sectionItemDef.withId("$id-create")
                sectionItemDef.withLabel(sectionNameById[id] ?: MLText(sectionItemDef.id))
                sectionItemDef.withType("SECTION")

                sectionItemDef.withItems(
                    variants.map {

                        val cvItemDef = MenuItemDef.create()
                        cvItemDef.withId("${it.typeRef}-${it.id}")

                        val config = ObjectData.create()

                        config["typeRef"] = it.typeRef.toString()
                        config["variantId"] = it.id
                        config["recordRef"] = it.typeRef
                        config["variantTypeRef"] = it.typeRef

                        config["variant"] = it

                        cvItemDef.withConfig(config)

                        cvItemDef.withType(ITEM_TYPE_LINK_CREATE_CASE)
                        cvItemDef.withLabel(it.name)
                        cvItemDef.build()
                    }
                )

                if (sectionItemDef.items.isNotEmpty()) {
                    result.add(sectionItemDef.build())
                }
            }

            return result
        }

        private fun extractCreateVariantsFromItem(
            item: MenuItemDef,
            result: MutableList<CreateVariantDef>,
            visitedTypes: MutableSet<EntityRef>
        ) {

            if (item.type == "SECTION") {
                item.items.forEach {
                    extractCreateVariantsFromItem(it, result, visitedTypes)
                }
            } else if (item.type == "JOURNAL") {

                val journalRef = item.config["recordRef"].asText()

                if (journalRef.isNotBlank()) {
                    val typeRef = ecosTypeService.getTypeRefByJournal(EntityRef.valueOf(journalRef))
                    if (EntityRef.isNotEmpty(typeRef) && visitedTypes.add(typeRef)) {
                        val typeInfo = ecosTypeService.getTypeInfo(typeRef)
                        result.addAll(typeInfo?.createVariants ?: emptyList())
                    }
                }
            }
        }

        private fun checkAllowedFor(allowedFor: List<String>): Boolean {
            if (allowedFor.isEmpty()) {
                return true
            }
            if (allowedFor.any { currentUserWithAuthorities.contains(it.lowercase()) }) {
                return true
            }
            return allowedFor.contains(ROLE_WS_MANAGER) &&
                isUserWsManager() ||
                allowedFor.contains(ROLE_WS_USER) &&
                isUserWsMember()
        }

        /**
         * Menu may be referenced in different ways ('menu@id', 'uiserv/menu@id', 'uiserv/rmenu@id'),
         * but for the recursion check we should compare menus by the same normalized form.
         */
        private fun normalizeMenuRef(menuRef: EntityRef): EntityRef {
            if (menuRef.getLocalId().isEmpty()) {
                return EntityRef.EMPTY
            }
            return menuRef.withSourceId(MenuRecords.ID).withDefaultAppName(AppName.UISERV)
        }

        private fun processMenuItems(
            subMenu: Map<String, SubMenuDef>
        ): Map<String, SubMenuDef> {

            val result = HashMap<String, SubMenuDef>()
            val contextsWithInternalIncludes = ArrayList<SubMenuProcessingContext>()

            for ((menuType, menu) in subMenu) {
                if (!checkAllowedFor(menu.allowedFor)) {
                    continue
                }
                val context = SubMenuProcessingContext(result, menuType)
                result[menuType] = processMenuItems(context, menu)
                if (context.internalIncludeItemsPaths.isNotEmpty()) {
                    contextsWithInternalIncludes.add(context)
                }
            }

            for (context in contextsWithInternalIncludes) {
                val subMenuData = result[context.subMenuType] ?: continue
                context.phase = SubMenuProcessingContext.ProcessingPhase.INTERNAL_INCLUDES
                result[context.subMenuType] = processMenuItems(context, subMenuData)
            }

            return result
        }

        private fun processMenuItems(
            context: SubMenuProcessingContext,
            menu: SubMenuDef
        ): SubMenuDef {
            val result = SubMenuDef()
            result.config = menu.config
            result.items = processMenuItems(context, menu.items)
            return result
        }

        private fun processMenuItems(
            context: SubMenuProcessingContext,
            items: List<MenuItemDef>
        ): List<MenuItemDef> {
            val result = ArrayList<MenuItemDef>()
            for (item in items) {
                context.doWithinSubPath(item.id) {
                    when (context.phase) {
                        SubMenuProcessingContext.ProcessingPhase.INITIAL -> {
                            initialMenuItemProcess(context, item, result)
                        }
                        SubMenuProcessingContext.ProcessingPhase.INTERNAL_INCLUDES -> {
                            internalIncludesMenuItemProcess(context, item, result)
                        }
                    }
                }
            }
            return result
        }

        private fun internalIncludesMenuItemProcess(
            context: SubMenuProcessingContext,
            item: MenuItemDef,
            result: MutableList<MenuItemDef>
        ) {

            if (item.type == ITEM_TYPE_CREATE_IN_SECTION) {

                val sectionId = item.config["sectionId"].asText()
                if (sectionId.isBlank()) {
                    log.warn { "$ITEM_TYPE_CREATE_IN_SECTION item without sectionId: $item" }
                    return
                }

                val section = findMenuItemById(context.subMenus["left"]?.items, sectionId)
                if (section == null) {
                    log.warn { "Section is not found by id: $sectionId" }
                } else {
                    result.addAll(evalCreateVariants(section))
                }
            } else {
                if (item.type == "SECTION") {
                    val newItems = ArrayList<MenuItemDef>()
                    item.items.forEach {
                        internalIncludesMenuItemProcess(context, it, newItems)
                    }
                    result.add(item.copy().withItems(newItems).build())
                } else {
                    result.add(item)
                }
            }
        }

        private fun initialMenuItemProcess(
            context: SubMenuProcessingContext,
            item: MenuItemDef,
            result: MutableList<MenuItemDef>
        ) {

            if (item.hidden) {
                return
            }
            if (!checkAllowedFor(item.allowedFor)) {
                return
            }

            val newItem = item.copy()

            if (item.type == ITEM_TYPE_LINK_CREATE_CASE) {
                val typeRef = EntityRef.valueOf(item.config["typeRef"].asText())
                var variantTypeRef = EntityRef.valueOf(item.config["variantTypeRef"].asText())
                if (EntityRef.isEmpty(variantTypeRef)) {
                    variantTypeRef = typeRef
                }
                val variantId = item.config["variantId"].asText()
                if (EntityRef.isNotEmpty(variantTypeRef)) {
                    val typeInfo = ecosTypeService.getTypeInfo(variantTypeRef)
                    val variant = typeInfo?.createVariants?.find {
                        it.id == variantId && it.typeRef == variantTypeRef
                    }
                    if (variant != null) {
                        val newConfig = ObjectData.deepCopyOrNew(item.config)
                        newConfig["variant"] = variant
                        newItem.withConfig(newConfig)
                        if (MLText.isEmpty(newItem.label)) {
                            newItem.withLabel(variant.name)
                        }
                    }
                }
            } else if (item.type == ITEM_TYPE_INCLUDE_MENU) {

                val menuRef = item.config["menuRef"].asText().toEntityRef()
                if (menuRef.isEmpty()) {
                    log.warn { "$ITEM_TYPE_INCLUDE_MENU item without menuRef: $item" }
                    return
                }

                val menuRefToInclude = normalizeMenuRef(menuRef)
                if (resolvingMenus.get().contains(menuRefToInclude)) {
                    log.warn {
                        "Recursive $ITEM_TYPE_INCLUDE_MENU was detected and will be skipped. " +
                            "Menu to include: '$menuRefToInclude' " +
                            "Menus in resolving: ${resolvingMenus.get()} " +
                            "SubMenu: '${context.subMenuType}' Item path: ${context.path}"
                    }
                    return
                }

                val configToInclude = doWithMenuInResolving(menuRefToInclude) {
                    recordsService.getAtt(
                        menuRef.withSourceId(ID),
                        "subMenu.${context.subMenuType}?json"
                    )
                }

                configToInclude.getAs(SubMenuDef::class.java)?.items?.let {
                    result.addAll(it)
                }
                return
            } else if (item.type == ITEM_TYPE_CREATE_IN_SECTION) {
                context.internalIncludeItemsPaths.add(context.path)
                result.add(item)
                return
            }

            newItem.withItems(processMenuItems(context, newItem.items))

            result.add(newItem.build())
        }
    }

    private class SubMenuProcessingContext(
        val subMenus: Map<String, SubMenuDef>,
        val subMenuType: String,
        var path: List<String> = emptyList(),
        var internalIncludeItemsPaths: MutableList<List<String>> = ArrayList(),
        var phase: ProcessingPhase = ProcessingPhase.INITIAL
    ) {
        inline fun <T> doWithinSubPath(subPath: String, action: () -> T): T {
            val newPath = ArrayList<String>(path.size + 1)
            newPath.addAll(path)
            newPath.add(subPath)
            val prevPath = path
            this.path = newPath
            try {
                return action.invoke()
            } finally {
                this.path = prevPath
            }
        }

        enum class ProcessingPhase {
            INITIAL,
            INTERNAL_INCLUDES
        }
    }
}
