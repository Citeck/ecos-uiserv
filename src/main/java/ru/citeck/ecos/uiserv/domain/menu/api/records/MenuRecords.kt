package ru.citeck.ecos.uiserv.domain.menu.api.records

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.annotation.PostConstruct
import lombok.Data
import lombok.RequiredArgsConstructor
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.commons.json.YamlUtils.toNonDefaultString
import ru.citeck.ecos.events2.type.RecordEventsService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.i18n.service.MessageResolver
import ru.citeck.ecos.uiserv.domain.menu.api.records.MenuRecords.MenuMutRecord
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors

@Component
@RequiredArgsConstructor
class MenuRecords(
    private val menuService: MenuService,
    private val messageResolver: MessageResolver
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordsQueryDao,
    RecordMutateDtoDao<MenuMutRecord>,
    RecordDeleteDao {

    companion object {
        const val ID = "menu"
        private const val AUTHORITIES_QUERY_LANG = "authorities"
    }

    private var recordEventsService: RecordEventsService? = null

    @PostConstruct
    fun init() {
        menuService.addOnChangeListener { before, after ->
            recordEventsService?.emitRecChanged(before, after, getId()) { MenuMutRecord(it) }
        }
    }

    override fun getId() = ID

    override fun getRecordAtts(recordId: String): MenuRecord {
        val menuDto = menuService.getMenu(recordId).orElseGet { MenuDto.EMPTY }
        return MenuRecord(menuDto)
    }

    override fun getRecToMutate(recordId: String): MenuMutRecord {
        return MenuMutRecord(getRecordAtts(recordId).model)
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<Any> {

        if (AUTHORITIES_QUERY_LANG == recsQuery.language) {
            return RecsQueryRes(ArrayList<Any>(menuService.allAuthoritiesWithMenu))
        }
        if (recsQuery.language == PredicateService.LANGUAGE_PREDICATE) {
            val predicate = recsQuery.getQuery(Predicate::class.java)
            val records = menuService.findAll(
                predicate,
                recsQuery.workspaces,
                recsQuery.page.maxItems,
                recsQuery.page.skipCount,
                recsQuery.sortBy
            ).map { MenuRecord(it) }
            val result = RecsQueryRes<Any>()
            result.setRecords(records)
            result.setTotalCount(menuService.getCount(predicate, recsQuery.workspaces))
            return result
        }
        if ("criteria" != recsQuery.language) {

            val menuQuery = recsQuery.getQueryOrNull(MenuQuery::class.java)

            if (menuQuery != null && StringUtils.isNotBlank(menuQuery.user)) {
                val menuDto = menuService.getMenuForCurrentUser(
                    menuQuery.version,
                    menuQuery.workspace ?: ""
                )
                return RecsQueryRes.of(MenuRecord(menuDto))
            }
        }

        val result = RecsQueryRes<Any>()
        result.setRecords(
            menuService.allMenus
                .stream()
                .map { model: MenuDto -> MenuRecord(model) }
                .collect(Collectors.toList())
        )
        return result
    }

    override fun saveMutatedRec(record: MenuMutRecord): String {
        require(!StringUtils.isBlank(record.id)) { "Parameter 'id' is mandatory for menu record" }
        if (MenuService.DEFAULT_MENUS.contains(record.id)) {
            record.id = UUID.randomUUID().toString()
        }
        val saved = menuService.save(record.build())
        return saved.id
    }

    override fun delete(recordId: String): DelStatus {
        menuService.deleteByExtId(recordId)
        return DelStatus.OK
    }

    @Autowired(required = false)
    fun setRecordEventsService(recordEventsService: RecordEventsService) {
        this.recordEventsService = recordEventsService
    }

    @Data
    class MenuQuery {
        val user: String? = null
        val version: Int? = null
        val workspace: String? = null
    }

    inner class MenuRecord(
        @AttName("...") val model: MenuDto
    ) {

        @AttName(RecordConstants.ATT_NOT_EXISTS)
        fun isNotExists(): Boolean {
            return model.id.isBlank()
        }

        fun getEcosType(): String {
            return "menu"
        }

        fun isDefaultMenu(): Boolean {
            return menuService.isDefaultMenu(model.id)
        }

        fun getDefaultMenuForJournal(): String {
            return messageResolver.getMessage(if (isDefaultMenu()) "label.yes" else "label.no")
        }

        fun getPermissions(): MenuPermissions {
            return MenuPermissions(!isDefaultMenu())
        }

        fun getModuleId(): String {
            return model.id
        }

        fun getDisplayName(): String {
            return model.id
        }

        fun getAuthoritiesForJournal(): String {
            val authorities = model.authorities
            return authorities.stream().filter { "GROUP_EVERYONE" != it }
                .collect(Collectors.joining(", "))
        }

        @JsonValue
        fun toJson(): Any {
            return model
        }

        fun getData(): ByteArray {
            return toNonDefaultString(toJson()).toByteArray(StandardCharsets.UTF_8)
        }
    }

    inner class MenuMutRecord(model: MenuDto?) : MenuDto.Builder(model ?: MenuDto.EMPTY) {

        override fun withSubMenu(subMenu: Map<String, SubMenuDef>?): MenuDto.Builder {
            val newSubMenu = HashMap(this.subMenu)
            subMenu?.forEach { (k, v) ->
                newSubMenu[k] = v
            }
            return super.withSubMenu(newSubMenu)
        }

        fun setModuleId(moduleId: String) {
            withId(moduleId)
        }

        @JsonProperty("_content")
        fun setContent(content: List<ObjectData>) {
            val dataUriContent = content[0].get("url", "")
            val data = mapper.read(dataUriContent, ObjectData::class.java)!!
            mapper.applyData(this, data)
        }
    }

    class MenuPermissions internal constructor(var editable: Boolean) : AttValue {
        override fun has(name: String): Boolean {
            return if ("Write" == name) {
                editable
            } else {
                true
            }
        }
    }
}
