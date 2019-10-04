package ru.citeck.ecos.uiserv.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.apps.EcosAppsFactory;
import ru.citeck.ecos.apps.module.type.impl.dashboard.DashboardModule;
import ru.citeck.ecos.apps.module.type.impl.form.FormModule;
import ru.citeck.ecos.apps.queue.EcosAppQueue;
import ru.citeck.ecos.apps.queue.EcosAppQueues;

@Configuration
public class EcosAppsFactoryConfig extends EcosAppsFactory {

    @Bean
    public Queue formPublishQueue() {
        EcosAppQueue queue = EcosAppQueues.getQueueForType(FormModule.TYPE);
        return new Queue(queue.getName());
    }

    @Bean
    public Queue dashboardPublishQueue() {
        EcosAppQueue queue = EcosAppQueues.getQueueForType(DashboardModule.TYPE);
        return new Queue(queue.getName());
    }
}
