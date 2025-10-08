package ru.citeck.ecos.uiserv.domain.journal.service

import jakarta.annotation.PostConstruct
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext.isRunAsSystem
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository
import ru.citeck.ecos.uiserv.domain.journal.service.mapper.JournalMapper
import ru.citeck.ecos.uiserv.domain.journal.service.provider.JournalsProvider
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.regex.Pattern

@Slf4j
@Service
@RequiredArgsConstructor
class JournalServiceImpl(
    private val journalRepository: JournalRepository,
    private val journalMapper: JournalMapper,
    private val jpaSearchConverterFactory: JpaSearchConverterFactory,
) : JournalService {

    companion object {
        private const val DEFAULT_AUTO_JOURNAL_FOR_TYPE = "DEFAULT_JOURNAL"

        var SYSTEM_JOURNALS: Set<String> = setOf(
            DEFAULT_AUTO_JOURNAL_FOR_TYPE
        )

        private const val VALID_ID_PATTERN_TXT = "^[\\w/.-]+\\w$"
        private val VALID_ID_PATTERN: Pattern = Pattern.compile(VALID_ID_PATTERN_TXT)

        private val VALID_COLUMN_NAME_PATTERN: Pattern = Pattern.compile(
            "^\\d?[a-zA-Z_][$\\da-zA-Z:_-]*$"
        )
        val VALID_COLUMN_ATT_PATTERN: Pattern = Pattern.compile(
            "^([a-zA-Z_][$.\\da-zA-Z:_-]*(\\(.+\\))?|\\(.+\\))$"
        )
    }

    private lateinit var searchConv: JpaSearchConverter<JournalEntity>

    private val changeListeners: MutableList<BiConsumer<JournalWithMeta?, JournalWithMeta?>> = CopyOnWriteArrayList()
    private val deleteListeners: MutableList<Consumer<JournalWithMeta>> = CopyOnWriteArrayList()

    private val providers: MutableMap<String, JournalsProvider> = ConcurrentHashMap()

    @PostConstruct
    fun init() {
        searchConv = jpaSearchConverterFactory.createConverter(JournalEntity::class.java).build()
    }

    override fun getLastModifiedTimeMs(): Long {
        return journalRepository.lastModifiedTime
            .map { it.toEpochMilli() }
            .orElse(0L)
    }

    override fun getJournalById(id: IdInWs): JournalWithMeta? {
        val localId = id.id
        if (localId.contains("$")) {
            val providerId = localId.substring(0, localId.indexOf('$'))
            val journalsProvider = providers[providerId]
            if (journalsProvider != null) {
                val journal = journalsProvider.getJournalById(
                    localId.substring(localId.indexOf('$') + 1)
                ) ?: return null

                var journalDef = journal.entity
                if (journalDef.id.isEmpty()) {
                    journalDef = journalDef.copy().withId(localId).build()
                }
                val journalWithMeta = JournalWithMeta(journalDef)
                journalWithMeta.created = journal.meta.created
                journalWithMeta.creator = journal.meta.creator
                journalWithMeta.modified = journal.meta.modified
                journalWithMeta.modifier = journal.meta.modifier
                return journalWithMeta
            }
        }
        val entity = journalRepository.findByExtIdAndWorkspace(localId, id.workspace)
        return entity.map { journalMapper.entityToDto(it) }.orElse(null)
    }

    override fun getAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<JournalWithMeta> {
        return searchConv.findAll(journalRepository, predicate, max, skip, sort)
            .map { journalMapper.entityToDto(it) }
    }

    override fun getAll(maxItems: Int, skipCount: Int): Set<JournalWithMeta> {
        if (maxItems <= 0) {
            return emptySet()
        }

        val page = PageRequest.of(
            skipCount / maxItems,
            maxItems,
            Sort.by(Sort.Direction.DESC, "id")
        )

        return journalRepository.findAll(page)
            .mapTo(LinkedHashSet()) { journalMapper.entityToDto(it) }
    }

    override fun getAll(extIds: Collection<IdInWs>): List<JournalWithMeta> {
        val idsToRequestFromDb: MutableMap<IdInWs, Int> = HashMap()
        val result: MutableList<Pair<JournalWithMeta, Int>> = ArrayList()
        for ((idx, id) in extIds.withIndex()) {
            val localId = id.id
            if (localId.contains("$")) {
                val providerId = localId.substring(0, localId.indexOf('$'))
                if (providers.containsKey(providerId)) {
                    val journalById = getJournalById(id)
                    if (journalById != null) {
                        result.add(Pair(journalById, idx))
                    }
                } else {
                    idsToRequestFromDb[id] = idx
                }
            } else {
                idsToRequestFromDb[id] = idx
            }
        }

        idsToRequestFromDb.keys.groupBy { it.workspace }.forEach { (workspace, identifiers) ->
            journalRepository.findAllByExtIdInAndWorkspace(
                identifiers.mapTo(HashSet()) { it.id },
                workspace
            ).map { journalMapper.entityToDto(it) }.forEach {
                val idInWs = IdInWs.create(workspace, it.journalDef.id)
                result.add(Pair(it, idsToRequestFromDb.getOrDefault(idInWs, 0)))
            }
        }

        result.sortBy { it.second }

        return result.map { it.first }
    }

    override fun getCount(): Long {
        return journalRepository.count()
    }

    override fun getCount(predicate: Predicate): Long {
        return searchConv.getCount(journalRepository, predicate)
    }

    override fun onJournalDeleted(consumer: Consumer<JournalWithMeta>) {
        deleteListeners.add(consumer)
    }

    override fun onJournalWithMetaChanged(consumer: BiConsumer<JournalWithMeta?, JournalWithMeta?>) {
        changeListeners.add(consumer)
    }

    override fun onJournalChanged(consumer: BiConsumer<JournalDef?, JournalDef?>) {
        changeListeners.add(
            BiConsumer { before: JournalWithMeta?, after: JournalWithMeta? ->
                consumer.accept(before?.journalDef, after?.journalDef)
            }
        )
    }

    override fun save(dto: JournalDef): JournalWithMeta {

        require(dto.id.isNotEmpty()) { "Journal without id: $dto" }
        require(!dto.id.contains("$")) { "You can't change generated journal: " + dto.id }

        if (SYSTEM_JOURNALS.contains(dto.id) && !isRunAsSystem()) {
            throw RuntimeException("You can't change system journal: " + dto.id)
        }

        // preprocess config with builder
        val dtoToSave = dto.copy().build()

        dtoToSave.columns.forEach(
            Consumer { column: JournalColumnDef ->
                val validNameMatcher = VALID_COLUMN_NAME_PATTERN.matcher(column.id)
                require(validNameMatcher.matches()) {
                    "Journal column name is invalid: '" + column.id + "'. Column: " + column
                }
                if (StringUtils.isNotBlank(column.attribute)) {
                    val validAttMatcher = VALID_COLUMN_ATT_PATTERN.matcher(column.attribute)
                    require(validAttMatcher.matches()) {
                        "Journal column attribute is invalid: '" + column.attribute + "'. Column: " + column
                    }
                }
            }
        )

        val valueBefore = journalRepository.findByExtIdAndWorkspace(dtoToSave.id, dtoToSave.workspace)
            .map { journalMapper.entityToDto(it) }
            .orElse(null)

        if (valueBefore == null && !VALID_ID_PATTERN.matcher(dtoToSave.id).matches()) {
            throw RuntimeException("Invalid id: " + dtoToSave.id)
        }

        val journalEntity = journalMapper.dtoToEntity(dtoToSave)
        val storedJournalEntity = journalRepository.save(journalEntity)

        val journalDto = journalMapper.entityToDto(storedJournalEntity)

        for (listener in changeListeners) {
            listener.accept(valueBefore, journalDto)
        }
        return journalDto
    }

    override fun delete(id: IdInWs) {
        val before = journalRepository.findByExtIdAndWorkspace(id.id, id.workspace)
        if (before.isPresent) {
            val beforeDto = journalMapper.entityToDto(before.get())
            journalRepository.delete(before.get())
            for (listener in deleteListeners) {
                listener.accept(beforeDto)
            }
        }
    }

    override fun registerProvider(provider: JournalsProvider) {
        providers[provider.getType()] = provider
    }
}
