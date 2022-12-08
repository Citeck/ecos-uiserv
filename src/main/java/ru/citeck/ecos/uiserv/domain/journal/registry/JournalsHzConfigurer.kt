package ru.citeck.ecos.uiserv.domain.journal.registry

import com.hazelcast.config.*
import org.springframework.stereotype.Component
import ru.citeck.ecos.webapp.lib.spring.context.hazelcast.HazelcastConfigurer

@Component
class JournalsHzConfigurer : HazelcastConfigurer {

    override fun configure(config: Config) {
        val map = ReplicatedMapConfig(JournalsRegistryConfiguration.JOURNALS_REGISTRY_SOURCE_ID)
        map.isAsyncFillup = false
        config.addReplicatedMapConfig(map)
    }
}
