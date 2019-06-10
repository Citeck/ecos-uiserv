package ru.citeck.ecos.uiserv.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = AlfrescoClientProperties.RIBBON_SERVICE_NAME, ignoreUnknownFields = true)
public class AlfrescoClientProperties {
    public static final String RIBBON_SERVICE_NAME = "alfresco";

    @Getter @Setter
    private String schema = "http";
}
