package ru.citeck.ecos.uiserv.domain.menu.api.records

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
import ecos.com.fasterxml.jackson210.annotation.JsonValue
import lombok.Data
import lombok.RequiredArgsConstructor
import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.op.delete.dao.RecordDeleteDao
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.i18n.service.I18nService
import ru.citeck.ecos.uiserv.domain.menu.api.records.MenuRecords.MenuRecord
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import java.util.*
import java.util.stream.Collectors

@Component
@RequiredArgsConstructor
class MenuRecords(
    private val menuService: MenuService,
    private val i18nService: I18nService
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordsQueryDao,
    RecordMutateDtoDao<MenuRecord>,
    RecordDeleteDao {

    companion object {
        const val ID = "menu"
        private const val AUTHORITIES_QUERY_LANG = "authorities"
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
                val menuDto = menuService.getMenuForUser(menuQuery.user, menuQuery.version)
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

    @Data
    class MenuQuery {
        val user: String? = null
        val version: Int? = null
    }

    inner class MenuRecord(model: MenuDto?) : MenuDto(model) {

        fun isDefaultMenu(): Boolean {
            return menuService.isDefaultMenu(id)
        }

        fun getDefaultMenuForJournal(): String {
            return i18nService.getMessage(if (isDefaultMenu()) "label.yes" else "label.no")
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
            var base64Content = content[0].get("url", "")
            base64Content = base64Content.replace("^data:application/json;base64,".toRegex(), "")
            val data = mapper.read(Base64.getDecoder().decode(base64Content), ObjectData::class.java)!!
            mapper.applyData(this, data)
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        fun toJson(): Any? {
            return MenuDto(this)
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
