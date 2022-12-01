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
import ru.citeck.ecos.webapp.api.lock.EcosLockService
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.time.Duration

@Configuration
class JournalsConfiguration(
    private val hazelcast: HazelcastInstance,
    private val appLockService: EcosLockService,
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

        val replicatedMap =
            hazelcast.getReplicatedMap<String, EntityWithMeta<JournalRegistryValue>>(JOURNALS_REGISTRY_SOURCE_ID)

        appLockService.doInSync("journals-registry-initializer", Duration.ofMinutes(10)) {
            initDataFromDb(replicatedMap)
            journalsService.onJournalWithMetaChanged { before, after ->
                var id = after?.journalDef?.id ?: ""
                if (id.isBlank()) {
                    id = before.journalDef.id
                }
                if (id.isNotBlank()) {
                    replicatedMap[id] = createRegistryValue(after)
                }
            }
            journalsService.onJournalDeleted {
                replicatedMap.remove(it.journalDef.id)
            }
            typesRegistry.initializationPromise().get()
            typesRegistry.listenEventsWithMeta { _, before, after ->
                val idBefore = before?.entity?.journalRef?.id ?: ""
                val idAfter = after?.entity?.journalRef?.id ?: ""
                if (idBefore != idAfter) {
                    if (after != null && idAfter.startsWith(TYPE_AUTO_JOURNAL_PREFIX)) {
                        replicatedMap[idAfter] = createRegistryValue(after)
                    } else if (idBefore.isNotBlank()) {
                        replicatedMap.remove(idBefore)
                    }
                }
            }
            typesRegistry.getAllValues().values.filter {
                it.entity.journalRef.id.startsWith(TYPE_AUTO_JOURNAL_PREFIX)
            }.forEach { value ->
                replicatedMap[value.entity.journalRef.id] = createRegistryValue(value)
            }
        }

        val config = ExtStorageRecordsDaoConfig.create(ReadOnlyMapExtStorage(replicatedMap))
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
                registry[it.journalDef.id] = createRegistryValue(it)
            }
            skipCount += journals.size
            journals = journalsService.getAll(100, skipCount)
        }
    }

    private fun createRegistryValue(typeDef: EntityWithMeta<TypeDef>): EntityWithMeta<JournalRegistryValue> {
        val def = typeJournalsProvider.createJournalDef(typeDef.entity)
        return EntityWithMeta(
            JournalRegistryValue(
                def.id,
                def.name,
                def.sourceId,
                def.typeRef,
                def.system
            ),
            typeDef.meta
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
