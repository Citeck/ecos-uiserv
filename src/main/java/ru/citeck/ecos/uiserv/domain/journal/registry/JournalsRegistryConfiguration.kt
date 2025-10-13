package ru.citeck.ecos.uiserv.domain.journal.registry

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.replicatedmap.ReplicatedMap
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.commons.data.entity.EntityMeta
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDao
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDaoConfig
import ru.citeck.ecos.records3.record.dao.impl.ext.impl.ReadOnlyMapExtStorage
import ru.citeck.ecos.uiserv.app.common.utils.TypeBasedAutoArtifactUtils
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.journal.api.records.JournalRecordsDao
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.uiserv.domain.journal.service.provider.TypeJournalsProvider
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.ifEmpty
import ru.citeck.ecos.webapp.api.lock.EcosLockApi
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.time.Duration

@Configuration
class JournalsRegistryConfiguration(
    private val hazelcast: HazelcastInstance,
    private val appLockService: EcosLockApi,
    private val journalsService: JournalService,
    private val typesRegistry: EcosTypesRegistry,
    private val ecosTypeService: EcosTypeService,
    private val workspaceService: WorkspaceService,
    private val typeJournalsProvider: TypeJournalsProvider
) {

    companion object {

        const val JOURNALS_REGISTRY_SOURCE_ID = "journals-registry"

        private const val TYPE_AUTO_JOURNAL_PREFIX = "type$"
    }

    @Bean
    fun journalsRegistryRecordsDao(): RecordsDao {

        val registry =
            hazelcast.getReplicatedMap<String, EntityWithMeta<JournalRegistryValue>>(JOURNALS_REGISTRY_SOURCE_ID)

        appLockService.doInSync("journals-registry-initializer", Duration.ofMinutes(10)) {
            initDataFromDb(registry)
            journalsService.onJournalWithMetaChanged { before, after ->
                val journalDef = after?.journalDef ?: before?.journalDef
                if (journalDef != null) {
                    val key = workspaceService.addWsPrefixToId(journalDef.id, journalDef.workspace)
                    setRegistryValue(registry, key, createRegistryValue(after))
                }
            }
            journalsService.onJournalDeleted {
                registry.remove(it.journalDef.id)
            }
            typesRegistry.initializationPromise().get()
            typesRegistry.listenEventsWithMeta { _, before, after ->
                TypeBasedAutoArtifactUtils.processTypeChanged(
                    registry,
                    before,
                    after,
                    TYPE_AUTO_JOURNAL_PREFIX,
                    { it.journalRef },
                    { createRegistryValue(it) }
                )
            }
            typesRegistry.getAllValues().values.filter {
                it.entity.journalRef.getLocalId().startsWith(TYPE_AUTO_JOURNAL_PREFIX)
            }.forEach { value ->
                setRegistryValue(registry, value.entity.journalRef.getLocalId(), createRegistryValue(value))
            }
        }

        val config = ExtStorageRecordsDaoConfig.create(ReadOnlyMapExtStorage(registry))
            .withSourceId(JOURNALS_REGISTRY_SOURCE_ID)
            .withEcosType("journal")
            .withWorkspaceScoped(true)
            .build()

        val dao = ExtStorageRecordsDao(config)
        dao.addAttributesMixin(JournalRegistryRecordsAttMixin())
        return dao
    }

    private fun initDataFromDb(registry: ReplicatedMap<String, EntityWithMeta<JournalRegistryValue>>) {
        var skipCount = 0
        var journals = journalsService.getAll(100, 0)
        while (journals.isNotEmpty()) {
            journals.forEach {
                val key = workspaceService.addWsPrefixToId(it.journalDef.id, it.journalDef.workspace)
                setRegistryValue(registry, key, createRegistryValue(it))
            }
            skipCount += journals.size
            journals = journalsService.getAll(100, skipCount)
        }
    }

    private fun setRegistryValue(
        registry: ReplicatedMap<String, EntityWithMeta<JournalRegistryValue>>,
        key: String,
        value: EntityWithMeta<JournalRegistryValue>?
    ) {
        if (value != null) {
            registry[key] = value
        } else {
            registry.remove(key)
        }
    }

    private fun createRegistryValue(typeDef: EntityWithMeta<TypeDef>): EntityWithMeta<JournalRegistryValue>? {
        val journalId = typeDef.entity.journalRef.getLocalId().substringAfter('$')
        val def = typeJournalsProvider.getJournalById(journalId) ?: return null
        return EntityWithMeta(
            JournalRegistryValue(
                def.entity.id,
                def.entity.name,
                def.entity.sourceId,
                def.entity.typeRef,
                normalizeWsForRegistry(def.entity.workspace),
                def.entity.system
            ),
            def.meta
        )
    }

    private fun normalizeWsForRegistry(workspace: String): String {
        if (workspace == ModelUtils.DEFAULT_WORKSPACE_ID) {
            return ""
        }
        return workspace
    }

    private fun createRegistryValue(journal: JournalWithMeta?): EntityWithMeta<JournalRegistryValue>? {
        journal ?: return null
        val typeRef = journal.journalDef.typeRef.ifEmpty {
            ecosTypeService.getTypeRefByJournal(
                EntityRef.create(
                    AppName.UISERV,
                    JournalRecordsDao.ID,
                    journal.journalDef.id
                )
            )
        }
        return EntityWithMeta(
            JournalRegistryValue(
                journal.journalDef.id,
                journal.journalDef.name,
                journal.journalDef.sourceId,
                typeRef,
                normalizeWsForRegistry(journal.journalDef.workspace),
                journal.journalDef.system
            ),
            EntityMeta(
                journal.created,
                journal.creator,
                journal.modified,
                journal.modifier
            )
        )
    }
}
