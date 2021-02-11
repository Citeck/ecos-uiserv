package ru.citeck.ecos.uiserv.domain.menu.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
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
    }

    override fun getId() = ID

    override fun getRecordAtts(record: String): Any? {
        return ResolvedMenu(menuRecords.getRecordAtts(record))
    }

    override fun queryRecords(query: RecordsQuery): Any? {

        val records = menuRecords.queryRecords(query)
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
            if (currentCreateMenu == null
                || currentCreateMenu.config.get("auto-variants").asBoolean(true)) {

                val createMenu = SubMenuDef()
                createMenu.config = ObjectData.create("""{"auto-variants":true}""")
                createMenu.items = evalCreateVariants(menu.subMenu["left"])
                subMenus["create"] = createMenu
            }

            return subMenus
        }

        private fun evalCreateVariants(leftMenu: SubMenuDef?): List<MenuItemDef> {

            leftMenu ?: return emptyList()

            val visitedTypes = HashSet<RecordRef>()

            val variants = mutableMapOf<String, MutableList<CreateVariantDef>>()
            val sectionNameById = mutableMapOf<String, MLText>()
            sectionNameById["_ROOT"] = MLText("Root")

            leftMenu.items?.forEach {
                if (it.type == "SECTION") {
                    sectionNameById[it.id] = it.label
                    val variantsRes = variants.computeIfAbsent(it.id) { ArrayList() }
                    extractCreateVariantsFromItem(it, variantsRes, visitedTypes)
                } else if (it.type == "JOURNAL") {
                    val variantsRes = variants.computeIfAbsent("_ROOT") { ArrayList() }
                    extractCreateVariantsFromItem(it, variantsRes, visitedTypes)
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
