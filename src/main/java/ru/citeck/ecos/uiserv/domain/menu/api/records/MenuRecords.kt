package ru.citeck.ecos.uiserv.domain.menu.api.records

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
import ecos.com.fasterxml.jackson210.annotation.JsonValue
import lombok.Data
import lombok.RequiredArgsConstructor
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.commons.json.YamlUtils.toNonDefaultString
import ru.citeck.ecos.events2.type.RecordEventsService
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
import ru.citeck.ecos.uiserv.domain.menu.api.records.MenuRecords.MenuRecord
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors
import javax.annotation.PostConstruct

@Component
@RequiredArgsConstructor
class MenuRecords(
    private val menuService: MenuService,
    private val messageResolver: MessageResolver
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordsQueryDao,
    RecordMutateDtoDao<MenuRecord>,
    RecordDeleteDao {

    companion object {
        const val ID = "menu"
        private const val AUTHORITIES_QUERY_LANG = "authorities"
    }

    private var recordEventsService: RecordEventsService? = null

    @PostConstruct
    fun init() {
        menuService.addOnChangeListener { before, after ->
            recordEventsService?.emitRecChanged(before, after, getId()) { MenuRecord(it) }
        }
    }

    override fun getId() = ID

    override fun getRecordAtts(record: String): MenuRecord {
        val menuDto = menuService.getMenu(record).orElseGet { MenuDto("") }
        return MenuRecord(menuDto)
    }

    override fun getRecToMutate(recordId: String): MenuRecord {
        return getRecordAtts(recordId)
    }

    override fun queryRecords(recordsQuery: RecordsQuery): RecsQueryRes<Any> {

        if (AUTHORITIES_QUERY_LANG == recordsQuery.language) {
            return RecsQueryRes(ArrayList<Any>(menuService.allAuthoritiesWithMenu))
        }
        if ("predicate" != recordsQuery.language && "criteria" != recordsQuery.language) {

            val menuQuery = recordsQuery.getQueryOrNull(MenuQuery::class.java)

            if (menuQuery != null && StringUtils.isNotBlank(menuQuery.user)) {
                val menuDto = menuService.getMenuForCurrentUser(/*menuQuery.user, */menuQuery.version)
                return RecsQueryRes.of(MenuRecord(menuDto))
            }
        }
        val result = RecsQueryRes<Any>()
        result.setRecords(menuService.allMenus
            .stream()
            .map { model: MenuDto? -> MenuRecord(model) }
            .collect(Collectors.toList()))
        return result
    }

    override fun saveMutatedRec(dto: MenuRecord): String {

        require(!StringUtils.isBlank(dto.id)) { "Parameter 'id' is mandatory for menu record" }
        if (dto.id == "default-menu" || dto.id == "default-menu-v1") {
            dto.id = UUID.randomUUID().toString()
        }
        val saved = menuService.save(dto)
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
    }

    inner class MenuRecord(model: MenuDto?) : MenuDto(model) {

        override fun setSubMenu(subMenu: MutableMap<String, SubMenuDef>?) {
            val newSubMenu = HashMap(this.subMenu)
            subMenu?.forEach { (k, v) ->
                newSubMenu[k] = v
            }
            super.setSubMenu(newSubMenu)
        }

        fun getEcosType(): String {
            return "menu"
        }

        fun isDefaultMenu(): Boolean {
            return menuService.isDefaultMenu(id)
        }

        fun getDefaultMenuForJournal(): String {
            return messageResolver.getMessage(if (isDefaultMenu()) "label.yes" else "label.no")
        }

        fun getPermissions(): MenuPermissions {
            return MenuPermissions(!isDefaultMenu())
        }

        fun getModuleId(): String? {
            return id
        }

        fun setModuleId(moduleId: String) {
            id = moduleId
        }

        override fun getVersion(): Int {
            val result = super.getVersion()
            return result ?: 0
        }

        @AttName(".disp")
        fun getDisplayName(): String {
            return id
        }

        fun getAuthoritiesForJournal(): String {
            val authorities = authorities ?: return ""
            return authorities.stream().filter { "GROUP_EVERYONE" != it }
                .collect(Collectors.joining(", "))
        }

        @JsonProperty("_content")
        fun setContent(content: List<ObjectData>) {
            val dataUriContent = content[0].get("url", "")
            val data = mapper.read(dataUriContent, ObjectData::class.java)!!
            mapper.applyData(this, data)
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        fun toJson(): Any? {
            return MenuDto(this)
        }

        fun getData(): ByteArray? {
            val json = toJson() ?: return null
            return toNonDefaultString(json).toByteArray(StandardCharsets.UTF_8)
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
