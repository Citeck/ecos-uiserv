package ru.citeck.ecos.uiserv.domain.journalsettings.service

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.data.entity.EntityMeta
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.data.AuthData
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.journalsettings.dao.JournalSettingsDao
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsRepository
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

@Service
@Transactional(
    rollbackFor = [Throwable::class],
    isolation = Isolation.READ_COMMITTED,
    propagation = Propagation.REQUIRED
)
class JournalSettingsServiceImpl(
    private val repo: JournalSettingsRepository,
    private val permService: JournalSettingsPermissionsService,
    private val journalSettingsDao: JournalSettingsDao,
    private val workspaceService: WorkspaceService
) : JournalSettingsService {

    companion object {
        const val ATT_CREATED_BY = "createdBy"
        const val ATT_WORKSPACES = "workspaces"
        const val ATT_AUTHORITIES = "authorities"
        const val ATT_JOURNAL_ID = "journalId"
    }

    private val listeners = CopyOnWriteArrayList<(JournalSettingsDto?, JournalSettingsDto?) -> Unit>()

    @Override
    override fun save(settings: JournalSettingsDto): JournalSettingsDto {

        var (entity, isNew) = toEntity(settings)

        val canWrite = if (isNew) {
            permService.canWriteNew(settings)
        } else {
            permService.canWrite(entity)
        }
        if (!canWrite) {
            throw IllegalAccessException("Access denied!")
        }

        entity = repo.save(entity)
        val newDto = toDto(entity)

        listeners.forEach {
            it.invoke(settings, newDto)
        }
        return newDto
    }

    override fun getDtoById(id: String): JournalSettingsDto? {
        return getById(id)?.entity
    }

    @Override
    override fun getById(id: String): EntityWithMeta<JournalSettingsDto>? {
        val entity = repo.findByExtId(id)
        if (entity != null && permService.canRead(entity)) {
            return toDtoWithMeta(entity)
        }
        return null
    }

    @Override
    override fun delete(id: String): Boolean {
        val entity: JournalSettingsEntity? = repo.findByExtId(id)
        if (entity != null) {
            if (!permService.canWrite(entity)) {
                throw IllegalAccessException("Access denied!")
            }
            repo.delete(entity)
            return true
        }
        return false
    }

    override fun getCount(predicate: Predicate, workspaces: List<String>): Long {
        return if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            journalSettingsDao.getCount(predicate)
        } else {
            0L
        }
    }

    override fun findAll(
        predicate: Predicate,
        workspaces: List<String>,
        max: Int,
        skip: Int,
        sort: List<SortBy>
    ): List<EntityWithMeta<JournalSettingsDto>> {
        return findAllImpl(predicate, workspaces, max, skip, sort, false)
    }

    private fun findAllImpl(
        predicate: Predicate,
        workspaces: List<String>,
        max: Int,
        skip: Int,
        sort: List<SortBy>,
        filterByAuthForAdmin: Boolean
    ): List<EntityWithMeta<JournalSettingsDto>> {

        val targetPredicate = Predicates.and(predicate)
        val currentUserAuth = AuthContext.getCurrentFullAuth()

        val wsPred = getWorkspacesPredicate(currentUserAuth, workspaces)
        if (PredicateUtils.isAlwaysFalse(wsPred)) {
            return emptyList()
        } else if (!PredicateUtils.isAlwaysTrue(wsPred)) {
            targetPredicate.addPredicate(wsPred)
        }

        if (!AuthContext.isSystemAuth(currentUserAuth) &&
            (!AuthContext.isAdminAuth(currentUserAuth) || filterByAuthForAdmin)
        ) {
            val authorities = mutableSetOf(currentUserAuth.getUser())
            authorities.addAll(currentUserAuth.getAuthorities())

            targetPredicate.addPredicate(
                Predicates.or(
                    Predicates.inVals(ATT_AUTHORITIES, authorities),
                    Predicates.eq(ATT_CREATED_BY, currentUserAuth.getUser())
                )
            )
        }

        return journalSettingsDao.findAll(targetPredicate, max, skip, sort).map { toDtoWithMeta(it) }
    }

    @Override
    override fun searchSettings(journalId: String, workspaces: List<String>): List<EntityWithMeta<JournalSettingsDto>> {
        val predicate = Predicates.eq(ATT_JOURNAL_ID, journalId)
        return findAllImpl(predicate, workspaces, -1, 0, emptyList(), true)
    }

    private fun getWorkspacesPredicate(currentUserAuth: AuthData, workspaces: List<String>): Predicate {
        val targetWorkspaces = if (workspaces.isEmpty()) {
            if (!currentUserAuth.isAdminOrSystem()) {
                workspaceService.getUserWorkspaces(currentUserAuth.getUser())
            } else {
                emptySet()
            }
        } else {
            val userWorkspaces = workspaceService.getUserWorkspaces(currentUserAuth.getUser())
            val filteredWorkspaces = workspaces.filter { userWorkspaces.contains(it) }
            if (filteredWorkspaces.isEmpty()) {
                return Predicates.alwaysFalse()
            }
            filteredWorkspaces
        }
        return if (targetWorkspaces.isNotEmpty()) {
            Predicates.or(
                Predicates.inVals(ATT_WORKSPACES, targetWorkspaces),
                Predicates.empty(ATT_WORKSPACES)
            )
        } else {
            Predicates.alwaysTrue()
        }
    }

    private fun AuthData.isAdminOrSystem(): Boolean {
        return AuthContext.isAdminAuth(this) || AuthContext.isSystemAuth(this)
    }

    override fun listenChanges(listener: (JournalSettingsDto?, JournalSettingsDto?) -> Unit) {
        this.listeners.add(listener)
    }

    private fun toEntity(dto: JournalSettingsDto): Pair<JournalSettingsEntity, Boolean> {
        val isNew: Boolean
        val settingsEntity: JournalSettingsEntity
        if (StringUtils.isBlank(dto.id)) {
            settingsEntity = JournalSettingsEntity()
            settingsEntity.extId = UUID.randomUUID().toString()
            isNew = true
        } else {
            val mutableSettingsEntity = repo.findByExtId(dto.id)
            if (mutableSettingsEntity != null) {
                settingsEntity = mutableSettingsEntity
                isNew = false
            } else {
                settingsEntity = JournalSettingsEntity()
                settingsEntity.extId = dto.id
                isNew = true
            }
        }
        settingsEntity.name = dto.name.toString()
        settingsEntity.settings = Json.mapper.toString(dto.settings)
        settingsEntity.journalId = dto.journalId
        settingsEntity.setWorkspacesForEntity(dto.workspaces)
        settingsEntity.setAuthoritiesForEntity(dto.authorities)
        return Pair(settingsEntity, isNew)
    }

    private fun toDtoWithMeta(entity: JournalSettingsEntity): EntityWithMeta<JournalSettingsDto> {
        val meta = EntityMeta(entity.createdDate, entity.createdBy, entity.lastModifiedDate, entity.lastModifiedBy)
        return EntityWithMeta(toDto(entity), meta)
    }

    private fun toDto(entity: JournalSettingsEntity): JournalSettingsDto {
        return JournalSettingsDto.create()
            .withId(entity.extId)
            .withName(Json.mapper.read(entity.name, MLText::class.java))
            .withWorkspaces(entity.workspaces)
            .withAuthorities(entity.authorities)
            .withJournalId(entity.journalId)
            .withSettings(Json.mapper.read(entity.settings, ObjectData::class.java))
            .withCreator(entity.createdBy)
            .build()
    }
}
