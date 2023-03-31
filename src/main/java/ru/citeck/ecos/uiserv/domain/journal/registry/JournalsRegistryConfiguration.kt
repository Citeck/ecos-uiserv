package ru.citeck.ecos.uiserv.domain.journal.registry

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.ReplicatedMap
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.commons.data.entity.EntityMeta
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDao
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDaoConfig
import ru.citeck.ecos.records3.record.dao.impl.ext.impl.ReadOnlyMapExtStorage
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.uiserv.domain.journal.service.provider.TypeJournalsProvider
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
                var id = after?.journalDef?.id ?: ""
                if (id.isBlank()) {
                    id = before.journalDef.id
                }
                if (id.isNotBlank()) {
                    setRegistryValue(registry, id, createRegistryValue(after))
                }
            }
            journalsService.onJournalDeleted {
                registry.remove(it.journalDef.id)
            }
            typesRegistry.initializationPromise().get()
            typesRegistry.listenEventsWithMeta { _, before, after ->
                val idBefore = before?.entity?.journalRef?.getLocalId() ?: ""
                val idAfter = after?.entity?.journalRef?.getLocalId() ?: ""
                if (idBefore != idAfter || before?.entity?.name != after?.entity?.name) {
                    if (after != null && idAfter.startsWith(TYPE_AUTO_JOURNAL_PREFIX)) {
                        setRegistryValue(registry, idAfter, createRegistryValue(after))
                    } else if (idBefore.isNotBlank()) {
                        registry.remove(idBefore)
                    }
                }
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
            .build()

        return ExtStorageRecordsDao(config)
    }

    private fun initDataFromDb(registry: ReplicatedMap<String, EntityWithMeta<JournalRegistryValue>>) {
        var skipCount = 0
        var journals = journalsService.getAll(100, 0)
        while (journals.isNotEmpty()) {
            journals.forEach {
                setRegistryValue(registry, it.journalDef.id, createRegistryValue(it))
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
                def.entity.system
            ),
            def.meta
        )
    }

    private fun createRegistryValue(journal: JournalWithMeta?): EntityWithMeta<JournalRegistryValue>? {
        journal ?: return null
        return EntityWithMeta(
            JournalRegistryValue(
                journal.journalDef.id,
                journal.journalDef.name,
                journal.journalDef.sourceId,
                journal.journalDef.typeRef,
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
