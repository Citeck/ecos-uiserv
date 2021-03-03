package ru.citeck.ecos.uiserv.domain.menu.api.records

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
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

        result.setRecords(records.getRecords().map {
            if (it is MenuRecords.MenuRecord) {
                ResolvedMenu(it)
            } else {
                it
            }
        })
        result.setTotalCount(records.getTotalCount())
        result.setHasMore(records.getHasMore())

        return result
    }

    inner class ResolvedMenu(
        @AttName("...")
        val menu: MenuRecords.MenuRecord
    ) {

        fun getSubMenu(): Map<String, SubMenuDef> {

            val subMenus = HashMap(menu.subMenu)

            val currentCreateMenu = subMenus["create"]

            if (currentCreateMenu == null) {

                val createMenu = SubMenuDef()
                createMenu.items = emptyList()
                subMenus["create"] = createMenu

            } else {

                val resultItems = ArrayList<MenuItemDef>()

                currentCreateMenu.items.forEach { createMenuItem ->

                    if (createMenuItem.type == "CREATE_IN_SECTION") {

                        val sectionId = createMenuItem.config.get("sectionId").asText()
                        if (sectionId.isNotBlank()) {

                            val section = findMenuItemById(subMenus["left"]?.items, sectionId)
                            if (section == null) {
                                log.warn("Section is not found by id: $sectionId")
                            }
                            evalCreateVariants(section).forEach {
                                resultItems.add(it)
                            }
                        } else {
                            log.warn("CREATE_IN_SECTION item without sectionId: $createMenuItem")
                        }
                    } else {
                        resultItems.add(createMenuItem)
                    }
                }

                val createMenu = SubMenuDef()
                createMenu.items = resultItems
                subMenus["create"] = createMenu
            }

            return subMenus
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

            val visitedTypes = HashSet<RecordRef>()

            val variants = mutableMapOf<String, MutableList<CreateVariantDef>>()
            val sectionNameById = mutableMapOf<String, MLText>()
            sectionNameById[section.id] = section.label

            section.items?.forEach { item0 ->

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

                val sectionItemDef = MenuItemDef()
                sectionItemDef.id = "$id-create"
                sectionItemDef.label = sectionNameById[id] ?: MLText(sectionItemDef.id)
                sectionItemDef.hidden = false
                sectionItemDef.type = "SECTION"

                sectionItemDef.items = variants.map {
                    val cvItemDef = MenuItemDef()
                    cvItemDef.id = "${it.typeRef}-${it.id}"
                    cvItemDef.config = ObjectData.create()
                    cvItemDef.config.set("recordRef", it.typeRef)
                    cvItemDef.config.set("variantId", it.id)
                    cvItemDef.config.set("variant", it)
                    cvItemDef.type = "LINK-CREATE-CASE"
                    cvItemDef.label = it.name
                    cvItemDef
                }

                if (sectionItemDef.items?.size ?: 0 > 0) {
                    result.add(sectionItemDef)
                }
            }

            return result
        }

        private fun extractCreateVariantsFromItem(item: MenuItemDef,
                                                  result: MutableList<CreateVariantDef>,
                                                  visitedTypes: MutableSet<RecordRef>) {

            if (item.type == "SECTION") {
                item.items?.forEach {
                    extractCreateVariantsFromItem(it, result, visitedTypes)
                }
            } else if (item.type == "JOURNAL") {

                val journalRef = item.config?.get("recordRef")?.asText()

                if (!journalRef.isNullOrBlank()) {
                    val typeRef = ecosTypeService.getTypeRefByJournal(RecordRef.valueOf(journalRef))
                    if (RecordRef.isNotEmpty(typeRef) && visitedTypes.add(typeRef)) {
                        val typeInfo = ecosTypeService.getTypeInfo(typeRef)
                        result.addAll(typeInfo?.inhCreateVariants ?: emptyList())
                    }
                }
            }
        }
    }
}
