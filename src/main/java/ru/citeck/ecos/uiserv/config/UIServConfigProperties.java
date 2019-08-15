package ru.citeck.ecos.uiserv.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "uiserv")
public class UIServConfigProperties {

    @Setter @Getter
    private Map<String, Config> config = Collections.emptyMap();

    @PostConstruct
    public void init() {
        config.forEach((k, v) -> v.setId(k));
    }

    public Optional<Config> getProperty(String key) {
        return Optional.ofNullable(config.get(key));
    }

    @Data
    public static class Config {
        private String id;
        private String title;
        private String description;
        private String value;
    }
}
