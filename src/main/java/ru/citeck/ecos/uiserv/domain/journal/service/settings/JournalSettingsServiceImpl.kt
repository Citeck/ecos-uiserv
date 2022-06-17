package ru.citeck.ecos.uiserv.domain.journal.service.settings

import org.apache.commons.lang.StringUtils
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.domain.file.repo.FileType
import ru.citeck.ecos.uiserv.domain.file.service.FileService
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsEntity
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsRepository
import ru.citeck.ecos.uiserv.domain.journal.service.JournalPrefService
import java.util.*
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
    private val journalPrefService: JournalPrefService,
    private val fileService: FileService
) : JournalSettingsService {

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
        return toDto(entity)
    }

    @Override
    override fun getById(id: String): JournalSettingsDto? {
        val entity = repo.findByExtId(id)
        if (entity != null) {
            if (permService.canRead(entity)) {
                return toDto(entity)
            }
            return null
        }

        val journalPrefs = journalPrefService.getJournalPrefs(id).orElse(null)
        return if (journalPrefs != null) {
            toDto(journalPrefs, null)
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

    @Override
    override fun searchSettings(journalId: String): List<JournalSettingsDto> {
        val searchSpecification = composeSearchSpecification(journalId)
        val foundedJournalSettings = repo.findAll(searchSpecification).stream()
            .map { toDto(it) }
            .collect(Collectors.toList())

        val foundedJournalPrefs = searchJournalPrefs(journalId)

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

    private fun mergeSettingsAndPrefs(settings: List<JournalSettingsDto>, prefs: List<JournalSettingsDto>): List<JournalSettingsDto> {
        val result = mutableListOf<JournalSettingsDto>()
        val prefsMap = prefs.map { it.id to it }.toMap()
        val settingsMap = settings.map { it.id to it }.toMap()
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

    private fun toDto(pref: JournalPrefService.JournalPreferences, journalId: String?): JournalSettingsDto {
        val currentUsername = getCurrentUsername()
        val prefSettings = ObjectData.create(pref.data)
        return JournalSettingsDto.create {
            withId(pref.fileId)
            withName(Json.mapper.read(prefSettings.get("title").asText(), MLText::class.java))
            withAuthority(currentUsername)
            withJournalId(journalId)
            withSettings(prefSettings)
            withCreator(currentUsername)
        }
    }

    private fun getCurrentUsername(): String {
        var username = AuthContext.getCurrentUser()
        require(!StringUtils.isBlank(username)) { "Username cannot be empty" }
        if (username.contains("people@")) {
            username = username.replaceFirst("people@".toRegex(), "")
        }
        if (username.contains("alfresco/")) {
            username = username.replaceFirst("alfresco/".toRegex(), "")
        }
        return username
    }
}
