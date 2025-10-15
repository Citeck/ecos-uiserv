package ru.citeck.ecos.uiserv.domain.dashdoard.service

import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto
import ru.citeck.ecos.uiserv.domain.dashdoard.repo.DashboardEntity
import ru.citeck.ecos.uiserv.domain.dashdoard.repo.DashboardRepository
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.BiConsumer
import java.util.stream.Collectors

@Service
class DashboardService(
    private val repo: DashboardRepository,
    private val recordsService: RecordsService,
    private val jpaSearchConverterFactory: JpaSearchConverterFactory,
    private val workspaceService: WorkspaceService
) {
    companion object {

        private const val DEFAULT_WORKSPACE_ID = "default"
        private val WORKSPACE_HOME_PAGE_TYPE = ModelUtils.getTypeRef("workspace-dashboard")
        private val DEFAULT_WS_HOME_PAGE_TYPE = ModelUtils.getTypeRef("user-dashboard")

        private val DEFAULT_DASHBOARDS = setOf(
            "personal-ws-dashboard-default",
            "ws-dashboard-default",
            "user-dashboard",
            "user-base-type-dashboard",
            "site-dashboard",
            "person-dashboard",
            "orgstructure-person-dashboard",
            "default-doclib-dashboard",
            "base-type-dashboard"
        )
    }

    private lateinit var searchConv: JpaSearchConverter<DashboardEntity>

    private val changeListeners: MutableList<BiConsumer<DashboardDto?, DashboardDto>> = CopyOnWriteArrayList()

    @PostConstruct
    fun init() {
        searchConv = jpaSearchConverterFactory.createConverter(DashboardEntity::class.java).build()
    }

    fun getCount(predicate: Predicate): Long {
        return searchConv.getCount(repo, predicate)
    }

    fun findAllForWorkspace(workspace: String): List<DashboardDto> {
        return findAll(Predicates.eq("workspace", workspace), emptyList(),1000, 0, emptyList())
    }

    fun findAll(predicate: Predicate, workspaces: List<String>, max: Int, skip: Int, sort: List<SortBy>): List<DashboardDto> {
        val predicateForQuery = Predicates.and(
            predicate,
            workspaceService.buildAvailableWorkspacesPredicate(AuthContext.getCurrentUser(), workspaces)
        )
        return searchConv.findAll(repo, predicateForQuery, max, skip, sort).stream()
            .map { entity: DashboardEntity -> this.mapToDto(entity) }
            .collect(Collectors.toList())
    }

    fun getAllDashboards(): List<DashboardDto> {
        return findAll(Predicates.alwaysTrue(), emptyList(), 1000, 0, emptyList())
    }

    fun getDashboardById(id: String?): Optional<DashboardDto> {
        return repo.findByExtId(id).map { entity: DashboardEntity -> this.mapToDto(entity) }
    }

    fun getForAuthority(
        recordRef: EntityRef?,
        type: EntityRef?,
        user: String?,
        scope: String?,
        workspace: String?,
        expandType: Boolean?,
        includeForAll: Boolean?
    ): Optional<DashboardDto> {
        var typeForSearch = type
        if (workspace == DEFAULT_WORKSPACE_ID && typeForSearch == WORKSPACE_HOME_PAGE_TYPE) {
            typeForSearch = DEFAULT_WS_HOME_PAGE_TYPE
        }
        if (!canUserSearchDashboardsInWorkspace(AuthContext.getCurrentUser(), workspace)) {
            return Optional.empty()
        }
        return findOneForWorkspace(workspace ?: "") { ws ->
            getEntityForUser(
                recordRef ?: EntityRef.EMPTY,
                typeForSearch ?: EntityRef.EMPTY,
                user ?: "",
                scope ?: "",
                ws,
                expandType ?: true,
                includeForAll ?: true
            )
        }.map { entity: DashboardEntity -> this.mapToDto(entity) }
    }

    private fun canUserSearchDashboardsInWorkspace(user: String, workspace: String?): Boolean {
        if (workspace.isNullOrEmpty() || AuthContext.SYSTEM_AUTH.getUser() == user) {
            return true
        }
        return workspaceService.isUserMemberOf(user, workspace)
    }

    fun saveDashboard(dashboard: DashboardDto): DashboardDto {
        var fixedDto = updateAuthority(dashboard)

        if (fixedDto.workspace.isEmpty() &&
            fixedDto.authority.isEmpty() &&
            DEFAULT_DASHBOARDS.contains(fixedDto.id) &&
            AuthContext.isNotRunAsSystemOrAdmin()
        ) {
            error("Permission denied. You can't change default dashboard ${fixedDto.id}")
        }

        if (fixedDto.workspace == DEFAULT_WORKSPACE_ID) {
            fixedDto = fixedDto.copy().withWorkspace("").build()
        }

        val entityBefore = findEntityForDto(fixedDto)
        val valueBefore = Optional.ofNullable(entityBefore)
            .map { entity: DashboardEntity -> this.mapToDto(entity) }
            .orElse(null)

        val entity = mapToEntity(fixedDto, entityBefore)
        val result = mapToDto(repo.save(entity))

        for (listener in changeListeners) {
            listener.accept(valueBefore, result)
        }
        return result
    }

    private fun updateAuthority(dashboard: DashboardDto): DashboardDto {

        val currentUserLogin = getCurrentUserLogin()
        val authority = dashboard.authority

        if (AuthContext.isRunAsSystemOrAdmin() || currentUserLogin == authority) {
            return dashboard
        }

        if (StringUtils.isBlank(authority)) {
            val ws = dashboard.workspace
            if (ws.isEmpty() || !workspaceService.isUserManagerOf(currentUserLogin, ws)) {
                return dashboard.copy().withAuthority(currentUserLogin).build()
            }
            return dashboard
        }
        throw AccessDeniedException(
            "User '" + currentUserLogin + "' can only change his dashboard. " +
                "But tried to change dashboard for authority '" + authority + "'"
        )
    }

    private fun getCurrentUserLogin(): String {
        val currentUserLogin = AuthContext.getCurrentUser()
        if (currentUserLogin.isEmpty()) {
            throw RuntimeException("User is not authenticated")
        }
        return currentUserLogin
    }

    fun removeDashboard(id: String?) {
        if (DEFAULT_DASHBOARDS.contains(id ?: "") && AuthContext.isNotRunAsSystem()) {
            error("You can't delete default dashboard '$id'")
        }
        repo.findByExtId(id).ifPresent { entity: DashboardEntity -> repo.delete(entity) }
    }

    private fun getEntityForUser(
        recordRef: EntityRef,
        type: EntityRef,
        user: String,
        scope: String,
        workspace: String,
        expandType: Boolean,
        includeForAll: Boolean
    ): Optional<DashboardEntity> {

        var authorities = if (StringUtils.isNotBlank(user)) {
            listOf(user)
        } else {
            emptyList()
        }

        authorities = authorities.stream()
            .map { obj: String -> obj.lowercase(Locale.ENGLISH) }
            .collect(Collectors.toList())

        var dashboards: List<DashboardEntity>
        if (!EntityRef.isEmpty(recordRef)) {

            var refForQuery = recordRef
            if (refForQuery.getAppName().isEmpty()) {
                refForQuery = recordRef.withAppName(AppName.ALFRESCO)
            }

            dashboards = findDashboardsByRecordRef(
                refForQuery.toString(),
                authorities,
                scope,
                workspace,
                includeForAll
            )
            if (dashboards.isNotEmpty()) {
                return dashboards.stream().findFirst()
            }
        }

        dashboards = findDashboardsByType(type.toString(), authorities, scope, workspace, includeForAll)
        if (dashboards.isEmpty() && expandType) {
            val typeMeta = recordsService.getAtts(type, ExpandedTypeMeta::class.java)
            for (parent in typeMeta.getParents()) {
                if (parent.inhDashboardType != typeMeta.inhDashboardType) {
                    break
                }
                dashboards = findDashboardsByType(
                    parent.id,
                    authorities,
                    scope,
                    workspace,
                    includeForAll
                )
                if (dashboards.isNotEmpty()) {
                    break
                }
            }
        }

        return dashboards.stream().findFirst()
    }

    private fun findDashboardsByRecordRef(
        recordRef: String,
        authorities: List<String>,
        scope: String,
        workspace: String,
        includeForAll: Boolean
    ): List<DashboardEntity> {
        if (authorities.isNotEmpty()) {
            val page = PageRequest.of(0, 1)
            var dashboards = repo.findForRefAndAuthorities(recordRef, authorities, scope, workspace, page)
            if (dashboards.isEmpty() && includeForAll) {
                dashboards = repo.findByRecordRefForAll(recordRef, scope, workspace)
                    .map { listOf(it) }
                    .orElse(emptyList())
            }
            return dashboards
        } else {
            val entity = repo.findByRecordRefForAll(recordRef, scope, workspace)
            return entity.map { listOf(it) }.orElse(emptyList())
        }
    }

    private fun findDashboardsByType(
        type: String,
        authorities: List<String>,
        scope: String,
        workspace: String,
        includeForAll: Boolean
    ): List<DashboardEntity> {
        val dashboards = if (authorities.isNotEmpty()) {
            val page = PageRequest.of(0, 1)
            var dashboards = repo.findForAuthorities(type, authorities, scope, workspace, page)
            if (dashboards.isEmpty() && includeForAll) {
                dashboards = repo.findByTypeRefForAll(type, scope, workspace)
                    .map { o: DashboardEntity? -> listOf(o) }
                    .orElse(emptyList())
            }
            dashboards
        } else {
            val entity = repo.findByTypeRefForAll(type, scope, workspace)
            entity.map { listOf(it) }.orElse(emptyList())
        }
        return dashboards
    }

    private inline fun findOneForWorkspace(
        workspace: String,
        findAction: (String) -> Optional<DashboardEntity>
    ): Optional<DashboardEntity> {
        val fixedWs = if (workspace == DEFAULT_WORKSPACE_ID) "" else workspace
        val result = findAction(fixedWs)
        if (result.isPresent || fixedWs.isEmpty()) {
            return result
        }
        return findAction("")
    }

    private fun mapToDto(entity: DashboardEntity): DashboardDto {
        return DashboardDto.create()
            .withId(entity.extId)
            .withName(mapper.read(entity.name, MLText::class.java))
            .withAuthority(entity.authority)
            .withConfig(
                mapper.read(
                    entity.config,
                    ObjectData::class.java
                )
            )
            .withPriority(entity.priority)
            .withScope(entity.scope)
            .withTypeRef(EntityRef.valueOf(entity.typeRef))
            .withAppliedToRef(EntityRef.valueOf(entity.appliedToRef))
            .withWorkspace(StringUtils.defaultString(entity.workspace))
            .build()
    }

    private fun findEntityForDto(dto: DashboardDto): DashboardEntity? {

        val optEntity: Optional<DashboardEntity?>
        val authority = getAuthorityFromDto(dto)
        var recordRef = dto.appliedToRef

        if (EntityRef.isNotEmpty(recordRef) && recordRef.getAppName().isEmpty()) {
            recordRef = recordRef.withAppName(AppName.ALFRESCO)
        }

        if (EntityRef.isEmpty(dto.typeRef) && EntityRef.isEmpty(dto.appliedToRef)) {
            optEntity = repo.findByExtId(dto.id)
        } else {
            val scope = StringUtils.defaultString(dto.scope)
            optEntity = if (authority == null) {
                if (EntityRef.isEmpty(recordRef)) {
                    repo.findByTypeRefForAll(dto.typeRef.toString(), scope, dto.workspace)
                } else {
                    repo.findByRecordRefForAll(recordRef.toString(), scope, dto.workspace)
                }
            } else {
                if (EntityRef.isEmpty(recordRef)) {
                    repo.findByAuthorityAndTypeRefAndScopeAndWorkspace(
                        authority,
                        dto.typeRef.toString(),
                        scope,
                        dto.workspace
                    )
                } else {
                    repo.findByAuthorityAndAppliedToRefAndScopeAndWorkspace(
                        authority,
                        recordRef.toString(),
                        scope,
                        dto.workspace
                    )
                }
            }
        }

        return optEntity.orElse(null)
    }

    private fun getAuthorityFromDto(dto: DashboardDto): String? {
        return if (StringUtils.isBlank(dto.authority)) null else dto.authority
    }

    private fun getAppliedToRefFromDto(dto: DashboardDto): EntityRef {
        var recordRef = dto.appliedToRef
        if (EntityRef.isNotEmpty(recordRef) && recordRef.getAppName().isEmpty()) {
            recordRef = recordRef.withAppName(AppName.ALFRESCO)
        }
        return recordRef
    }

    private fun mapToEntity(dto: DashboardDto, entity: DashboardEntity?): DashboardEntity {

        var entityRes = entity ?: findEntityForDto(dto)

        val appliedToRef = getAppliedToRefFromDto(dto)

        if (entityRes == null) {
            val newDashboard = DashboardEntity()

            var extId = dto.id
            if (StringUtils.isNotBlank(extId)) {
                if (repo.findByExtId(extId).isPresent) {
                    extId = ""
                }
            }
            if (StringUtils.isBlank(extId)) {
                extId = UUID.randomUUID().toString()
            }

            newDashboard.extId = extId
            newDashboard.authority = getAuthorityFromDto(dto)
            newDashboard.typeRef = EntityRef.toString(dto.typeRef)
            if (EntityRef.isNotEmpty(appliedToRef)) {
                newDashboard.appliedToRef = EntityRef.toString(appliedToRef)
            }
            newDashboard.scope = StringUtils.defaultString(dto.scope)
            newDashboard.workspace = dto.workspace
            entityRes = newDashboard
        }

        if (!dto.config.isEmpty()) {
            entityRes.config = mapper.toBytes(dto.config)
        }
        if (!MLText.isEmpty(dto.name)) {
            entityRes.name = mapper.toString(dto.name)
        }
        if (StringUtils.isNotBlank(dto.scope)) {
            entityRes.scope = dto.scope
        }

        return entityRes
    }

    fun addChangeListener(changeListener: BiConsumer<DashboardDto?, DashboardDto>) {
        changeListeners.add(changeListener)
    }

    private class ExpandedTypeMeta(
        private val parents: List<ParentMeta>? = null,
        val inhDashboardType: String? = null
    ) {

        fun getParents(): List<ParentMeta> {
            return parents ?: emptyList()
        }
    }

    class ParentMeta(
        @AttName("?id")
        val id: String,
        val inhDashboardType: String? = null
    )
}
