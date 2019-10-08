package ru.citeck.ecos.uiserv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.citeck.ecos.apps.spring.EcosAppsFactoryConfig;

@Configuration
@Import(EcosAppsFactoryConfig.class)
public class ApplicationConfig {
}
