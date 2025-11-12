package ru.citeck.ecos.uiserv;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import ru.citeck.ecos.uiserv.app.application.props.ApplicationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import ru.citeck.ecos.webapp.lib.spring.EcosSpringApplication;

@SpringBootApplication
@EnableConfigurationProperties({LiquibaseProperties.class, ApplicationProperties.class})
@EnableDiscoveryClient
@EnableMethodSecurity(securedEnabled = true)
@EnableJpaRepositories({
    "ru.citeck.ecos.uiserv.app.*.repo",
    "ru.citeck.ecos.uiserv.domain.*.repo"
})
public class Application {

    public static final String NAME = "uiserv";

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new EcosSpringApplication(Application.class).run(args);
    }
}
