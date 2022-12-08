package ru.citeck.ecos.uiserv.domain.form.registry

import com.hazelcast.config.Config
import com.hazelcast.config.ReplicatedMapConfig
import org.springframework.stereotype.Component
import ru.citeck.ecos.webapp.lib.spring.context.hazelcast.HazelcastConfigurer

@Component
class FormsHzConfigurer : HazelcastConfigurer {

    override fun configure(config: Config) {
        val map = ReplicatedMapConfig(FormsRegistryConfiguration.FORMS_REGISTRY_SOURCE_ID)
        map.isAsyncFillup = false
        config.addReplicatedMapConfig(map)
    }
}
