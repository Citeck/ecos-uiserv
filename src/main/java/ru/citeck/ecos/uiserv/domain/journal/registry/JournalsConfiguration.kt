package ru.citeck.ecos.uiserv.domain.journal.registry

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext
import ru.citeck.ecos.webapp.lib.registry.EcosRegistry
import ru.citeck.ecos.webapp.lib.registry.EcosRegistryImpl
import ru.citeck.ecos.webapp.lib.registry.init.ZkRegistryInitializer
import ru.citeck.ecos.webapp.lib.registry.records.EcosRegistryRecordsDao
import ru.citeck.ecos.zookeeper.EcosZooKeeper

@Configuration
class JournalsConfiguration(
    private val ecosWebAppContext: EcosWebAppContext,
    private val ecosZooKeeper: EcosZooKeeper
) {

    companion object {
        const val JOURNALS_REGISTRY_SOURCE_ID = "journals-registry"
    }

    @Bean
    fun createJournalsRegistry(initializer: JournalsRegistryInitializer): EcosRegistry<JournalRegistryValue> {
        return EcosRegistryImpl(
            "journals",
            JournalRegistryValue::class.java,
            listOf(
                initializer,
                ZkRegistryInitializer(
                    ecosWebAppContext.getAppLockService(),
                    ecosZooKeeper,
                    false,
                    ecosWebAppContext.getTasksApi().getMainScheduler()
                )
            )
        )
    }

    @Bean
    fun createJournalsRegistryRecordsDao(registry: EcosRegistry<JournalRegistryValue>): RecordsDao {
        return EcosRegistryRecordsDao(JOURNALS_REGISTRY_SOURCE_ID, registry)
    }
}
