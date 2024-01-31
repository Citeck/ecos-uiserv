package ru.citeck.ecos.uiserv.domain.journalsettings.service

import org.apache.commons.lang.StringUtils
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
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.file.repo.FileType
import ru.citeck.ecos.uiserv.domain.file.service.FileService
import ru.citeck.ecos.uiserv.domain.journal.service.JournalPrefService
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsRepository
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Collectors
import javax.annotation.PostConstruct

@Service
@Transactional(
    rollbackFor = [Throwable::class],
    isolation = Isolation.READ_COMMITTED,
    propagation = Propagation.REQUIRED
)
class JournalSettingsServiceImpl(
    private val repo: JournalSettingsRepository,
    private val permService: JournalSettingsPermissionsService,
    private val journalPrefService: JournalPrefService,
    private val fileService: FileService,
    private val jpaSearchConverterFactory: JpaSearchConverterFactory
) : JournalSettingsService {

    private val listeners = CopyOnWriteArrayList<(JournalSettingsDto?, JournalSettingsDto?) -> Unit>()

    private lateinit var searchConv: JpaSearchConverter<JournalSettingsEntity>

    @PostConstruct
    fun init() {
        searchConv = jpaSearchConverterFactory.createConverter(JournalSettingsEntity::class.java).build()
    }

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

    @Override
    override fun getById(id: String): EntityWithMeta<JournalSettingsDto>? {
        val entity = repo.findByExtId(id)
        if (entity != null) {
            if (permService.canRead(entity)) {
                return toDtoWithMeta(entity)
            }
            return null
        }

        val journalPrefs = journalPrefService.getJournalPrefs(id).orElse(null)
        return if (journalPrefs != null) {
            EntityWithMeta(toDto(journalPrefs, null))
        } else {
            null
        }
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

        val oldPrefs = journalPrefService.getJournalPrefs(id).orElse(null)
        if (oldPrefs != null) {
            fileService.delete(FileType.JOURNALPREFS, id)
            return true
        }

        return false
    }

    override fun getCount(predicate: Predicate): Long {
        return if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            searchConv.getCount(repo, predicate)
        } else {
            0L
        }
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>):
        List<EntityWithMeta<JournalSettingsDto>> {
        return if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            searchConv.findAll(repo, predicate, max, skip, sort).map { toDtoWithMeta(it) }
        } else {
            emptyList()
        }
    }

    @Override
    override fun searchSettings(journalId: String): List<EntityWithMeta<JournalSettingsDto>> {
        val searchSpecification = composeSearchSpecification(journalId)
        val foundedJournalSettings = repo.findAll(searchSpecification).stream()
            .map { toDtoWithMeta(it) }
            .collect(Collectors.toList())

        val foundedJournalPrefs = searchJournalPrefs(journalId).map { EntityWithMeta(it) }

        return mergeSettingsAndPrefs(foundedJournalSettings, foundedJournalPrefs)
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

    private fun searchJournalPrefs(journalId: String): List<JournalSettingsDto> {
        val result = mutableListOf<JournalSettingsDto>()
        val currentUsername = getCurrentUsername()
        val prefs = journalPrefService.find(journalId, currentUsername, true)
        for (pref in prefs) {
            result.add(toDto(pref, journalId))
        }
        return result
    }

    private fun mergeSettingsAndPrefs(settings: List<EntityWithMeta<JournalSettingsDto>>,
                                      prefs: List<EntityWithMeta<JournalSettingsDto>>): List<EntityWithMeta<JournalSettingsDto>> {
        val result = mutableListOf<EntityWithMeta<JournalSettingsDto>>()
        val prefsMap = prefs.associateBy { it.entity.id }
        val settingsMap = settings.associateBy { it.entity.id }
        prefsMap.forEach { id, dto ->
            if (!settingsMap.containsKey(id)) {
                result.add(dto)
            }
        }
        result.addAll(settings)
        return result
    }

    @Override
    override fun getSettings(authority: String?, journalId: String?): List<JournalSettingsDto> {
        val configs: List<JournalSettingsEntity> = repo.findAllByAuthorityAndJournalId(authority, journalId)
        return configs.stream()
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
        settingsEntity.authority = dto.authority
        return Pair(settingsEntity, isNew)
    }

    private fun toDto(entity: JournalSettingsEntity): JournalSettingsDto {
        return JournalSettingsDto.create()
            .withId(entity.extId)
            .withName(Json.mapper.read(entity.name, MLText::class.java))
            .withAuthority(entity.authority)
            .withJournalId(entity.journalId)
            .withSettings(Json.mapper.read(entity.settings, ObjectData::class.java))
            .withCreator(entity.createdBy)
            .build()
    }

    private fun toDtoWithMeta(entity: JournalSettingsEntity): EntityWithMeta<JournalSettingsDto> {
        val dto = JournalSettingsDto.create()
            .withId(entity.extId)
            .withName(Json.mapper.read(entity.name, MLText::class.java))
            .withAuthority(entity.authority)
            .withJournalId(entity.journalId)
            .withSettings(Json.mapper.read(entity.settings, ObjectData::class.java))
            .withCreator(entity.createdBy)
            .build()

        val meta = EntityMeta(entity.createdDate, entity.createdBy, entity.lastModifiedDate, entity.lastModifiedBy)

        return EntityWithMeta(dto, meta)
    }

    private fun toDto(pref: JournalPrefService.JournalPreferences, journalId: String?): JournalSettingsDto {
        val currentUsername = getCurrentUsername()
        val prefSettings = ObjectData.create(pref.data)
        return JournalSettingsDto.create {
            withId(pref.fileId)
            withName(Json.mapper.read(prefSettings["title"].asText(), MLText::class.java))
            withAuthority(currentUsername)
            withJournalId(journalId)
            withSettings(prefSettings)
            withCreator(currentUsername)
        }
    }

    private fun getCurrentUsername(): String {
        val username = AuthContext.getCurrentUser()
        require(!StringUtils.isBlank(username)) { "Username cannot be empty" }
        return username
    }
}
