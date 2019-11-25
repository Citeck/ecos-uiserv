package ru.citeck.ecos.uiserv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.records2.spring.RecordsProperties;

@Configuration
public class ApplicationConfig {

    @Bean
    public RecordsProperties recordsProperties() {

        RecordsProperties.AlfProps alfProps = new RecordsProperties.AlfProps();

        RecordsProperties.Authentication auth = new RecordsProperties.Authentication();
        alfProps.setAuth(auth);

        RecordsProperties recProps = new RecordsProperties();
        recProps.setAlfresco(alfProps);

        return recProps;
    }
}
