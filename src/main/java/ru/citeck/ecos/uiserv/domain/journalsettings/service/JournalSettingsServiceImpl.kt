package ru.citeck.ecos.uiserv.domain.journalsettings.service

import org.apache.commons.lang3.StringUtils
import org.springframework.data.jpa.domain.Specification
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
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.journalsettings.dao.JournalSettingsDao
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsRepository
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Collectors

@Service
@Transactional(
    rollbackFor = [Throwable::class],
    isolation = Isolation.READ_COMMITTED,
    propagation = Propagation.REQUIRED
)
class JournalSettingsServiceImpl(
    private val repo: JournalSettingsRepository,
    private val permService: JournalSettingsPermissionsService,
    private val journalSettingsDao: JournalSettingsDao
) : JournalSettingsService {

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

    override fun getCount(predicate: Predicate): Long {
        return if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            journalSettingsDao.getCount(predicate)
        } else {
            0L
        }
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<EntityWithMeta<JournalSettingsDto>> {
        return if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            journalSettingsDao.findAll(predicate, max, skip, sort).map { toDtoWithMeta(it) }
        } else {
            val currentUserAuthorities = AuthContext.getCurrentUserWithAuthorities()
            journalSettingsDao.findAll(predicate, max, skip, sort)
                .filter { compareAuthorities(it, currentUserAuthorities) }
                .map { toDtoWithMeta(it) }
        }
    }

    @Override
    override fun searchSettings(journalId: String): List<EntityWithMeta<JournalSettingsDto>> {
        return getJournalSettingsForCurrentUser(journalId)
    }

    fun getJournalSettingsForCurrentUser(journalId: String): List<EntityWithMeta<JournalSettingsDto>> {
        val currentUserAuthorities = AuthContext.getCurrentUserWithAuthorities()
        val predicate = searchPredicate(journalId)
        return journalSettingsDao.findAll(predicate, -1, 0, emptyList())
            .filter { compareAuthorities(it, currentUserAuthorities) }
            .map { toDtoWithMeta(it) }
    }

    private fun searchPredicate(journalId: String): Predicate {
        return Predicates.eq("journalId", journalId)
    }

    private fun compareAuthorities(
        journalSettingsEntity: JournalSettingsEntity,
        currentUserAuthorities: List<String>
    ): Boolean {
        val createdBy = journalSettingsEntity.createdBy
        if (AuthContext.isRunAsAdmin()) {
            if (journalSettingsEntity.authorities?.contains(createdBy) == false &&
                createdBy != journalSettingsEntity.authority
            ) {
                return true
            }
        }

        if (currentUserAuthorities.contains(createdBy)) {
            return true
        }

        if (currentUserAuthorities.contains(journalSettingsEntity.authority)) {
            return true
        }

        return journalSettingsEntity.authorities?.stream()
            ?.anyMatch { currentUserAuthorities.contains(it) } ?: false
    }

    private fun composeSearchSpecification(journalId: String): Specification<JournalSettingsEntity> {
        val currentUserAuthorities = AuthContext.getCurrentUserWithAuthorities()

        var specification = JournalSettingsSpecification.journalEquals(journalId)

        if (AuthContext.isRunAsAdmin()) {
            specification = specification.and(
                JournalSettingsSpecification.authorityIn(currentUserAuthorities)
                    .or(JournalSettingsSpecification.authorityNotEqualToCreator())
            )
        } else {
            specification = specification.and(
                JournalSettingsSpecification.authorityIn(currentUserAuthorities)
            )
        }
        return specification
    }

    @Override
    @Deprecated("use searchSettings method instead of this")
    override fun getSettings(authority: String?, journalId: String?): List<JournalSettingsDto> {
        var configsSet: Set<JournalSettingsEntity> = repo.findAllByAuthorityAndJournalId(authority, journalId).toSet()
        configsSet = configsSet.plus(repo.findAllByAuthoritiesInAndJournalId(authority, journalId))
        return configsSet.stream()
            .map { entity: JournalSettingsEntity -> toDto(entity) }
            .collect(Collectors.toList())
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
        settingsEntity.authority = dto.getAuthority()
        settingsEntity.setAuthoritiesForEntity(dto.authorities)
        return Pair(settingsEntity, isNew)
    }

    private fun toDto(entity: JournalSettingsEntity): JournalSettingsDto {
        return JournalSettingsDto.create()
            .withId(entity.extId)
            .withName(Json.mapper.read(entity.name, MLText::class.java))
            .withAuthorities(getAuthorities(entity))
            .withJournalId(entity.journalId)
            .withSettings(Json.mapper.read(entity.settings, ObjectData::class.java))
            .withCreator(entity.createdBy)
            .build()
    }

    private fun getAuthorities(entity: JournalSettingsEntity): List<String>? {
        val authorities = entity.authorities
        if (!authorities.isNullOrEmpty()) {
            return authorities
        }
        val authority = entity.authority
        return if (authority.isNullOrBlank()) {
            null
        } else {
            listOf(authority)
        }
    }

    private fun toDtoWithMeta(entity: JournalSettingsEntity): EntityWithMeta<JournalSettingsDto> {
        val dto = JournalSettingsDto.create()
            .withId(entity.extId)
            .withName(Json.mapper.read(entity.name, MLText::class.java))
            .withAuthorities(getAuthorities(entity))
            .withJournalId(entity.journalId)
            .withSettings(Json.mapper.read(entity.settings, ObjectData::class.java))
            .withCreator(entity.createdBy)
            .build()

        val meta = EntityMeta(entity.createdDate, entity.createdBy, entity.lastModifiedDate, entity.lastModifiedBy)

        return EntityWithMeta(dto, meta)
    }
}
