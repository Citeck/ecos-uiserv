package ru.citeck.ecos.uiserv.domain.menu.api.records

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@Component
class ResolvedMenuRecords(
    private val menuRecords: MenuRecords,
    private val ecosTypeService: EcosTypeService
) : AbstractRecordsDao(), RecordAttsDao, RecordsQueryDao {

    companion object {
        const val ID = "rmenu"

        private val log = KotlinLogging.logger {}
    }

    override fun getId() = ID

    override fun getRecordAtts(recordId: String): Any? {
        return ResolvedMenu(menuRecords.getRecordAtts(recordId))
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        val records = menuRecords.queryRecords(recsQuery)
        val result = RecsQueryRes<Any>()

        result.setRecords(
            records.getRecords().map {
                if (it is MenuRecords.MenuRecord) {
                    ResolvedMenu(it)
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
        val menu: MenuRecords.MenuRecord
    ) {

        fun getSubMenu(): SubMenus {
            return SubMenus(menu.subMenu)
        }
    }

    inner class SubMenus(originalDef: Map<String, SubMenuDef>) {

        private val subMenus: Map<String, SubMenuDef>

        init {
            val authorities = HashSet(ArrayList(AuthContext.getCurrentUserWithAuthorities()))
            subMenus = processMenuItems(originalDef, authorities.map { it.lowercase() }.toSet())
        }

        fun getUser(): SubMenuDef {
            return subMenus["user"] ?: SubMenuDef()
        }

        fun getLeft(): SubMenuDef {
            return subMenus["left"] ?: SubMenuDef()
        }

        fun getCreate(): SubMenuDef {

            val currentCreateMenu = subMenus["create"]

            return if (currentCreateMenu == null) {

                val createMenu = SubMenuDef()
                createMenu.items = emptyList()
                createMenu
            } else {

                val createMenu = SubMenuDef()
                createMenu.items = preProcessSubMenuItems(currentCreateMenu.items)
                createMenu
            }
        }

        private fun preProcessSubMenuItems(items: List<MenuItemDef>): List<MenuItemDef> {

            val resultItems = ArrayList<MenuItemDef>()

            items.forEach { createMenuItem ->

                if (createMenuItem.type == "CREATE_IN_SECTION") {

                    val sectionId = createMenuItem.config.get("sectionId").asText()
                    if (sectionId.isNotBlank()) {

                        val section = findMenuItemById(subMenus["left"]?.items, sectionId)
                        if (section == null) {
                            log.warn { "Section is not found by id: $sectionId" }
                        }
                        evalCreateVariants(section).forEach {
                            resultItems.add(it)
                        }
                    } else {
                        log.warn { "CREATE_IN_SECTION item without sectionId: $createMenuItem" }
                    }
                } else if (createMenuItem.type == "SECTION") {

                    resultItems.add(
                        createMenuItem.copy()
                            .withItems(preProcessSubMenuItems(createMenuItem.items))
                            .build()
                    )
                } else {
                    resultItems.add(createMenuItem)
                }
            }
            return resultItems
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

                        config.set("typeRef", it.typeRef.toString())
                        config.set("variantId", it.id)
                        config.set("recordRef", it.typeRef)
                        config.set("variantTypeRef", it.typeRef)

                        config.set("variant", it)

                        cvItemDef.withConfig(config)

                        cvItemDef.withType("LINK-CREATE-CASE")
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

                val journalRef = item.config.get("recordRef").asText()

                if (journalRef.isNotBlank()) {
                    val typeRef = ecosTypeService.getTypeRefByJournal(EntityRef.valueOf(journalRef))
                    if (EntityRef.isNotEmpty(typeRef) && visitedTypes.add(typeRef)) {
                        val typeInfo = ecosTypeService.getTypeInfo(typeRef)
                        result.addAll(typeInfo?.createVariants ?: emptyList())
                    }
                }
            }
        }

        private fun processMenuItems(
            subMenu: Map<String, SubMenuDef>,
            authorities: Set<String>
        ): Map<String, SubMenuDef> {

            val result = HashMap<String, SubMenuDef>()
            subMenu.forEach { (menuType, menu) ->
                result[menuType] = processMenuItems(menu, authorities)
            }
            return result
        }

        private fun processMenuItems(menu: SubMenuDef, authorities: Set<String>): SubMenuDef {
            val result = SubMenuDef()
            result.config = menu.config
            result.items = processMenuItems(menu.items, authorities)
            return result
        }

        private fun processMenuItems(items: List<MenuItemDef>, authorities: Set<String>): List<MenuItemDef> {
            return items.mapNotNull { processMenuItem(it, authorities) }
        }

        private fun processMenuItem(item: MenuItemDef, authorities: Set<String>): MenuItemDef? {

            if (item.hidden) {
                return null
            }
            if (item.allowedFor.isNotEmpty() && !item.allowedFor.any { authorities.contains(it.lowercase()) }) {
                return null
            }

            val newItem = item.copy()

            if (item.type == "LINK-CREATE-CASE") {
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
            }

            newItem.withItems(processMenuItems(newItem.items, authorities))

            return newItem.build()
        }
    }
}
