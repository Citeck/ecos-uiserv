package ru.citeck.ecos.uiserv.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Properties specific to My App.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Getter
@Setter
public class ApplicationProperties {
    private List<String> menuConfigAuthorityOrder;
    private String defaultThemeId;
    private String tryHeaderForUsername;

    public void setMenuConfigAuthorityOrder(String orderString) {
        this.menuConfigAuthorityOrder = Arrays.stream(orderString.split(","))
            .filter(StringUtils::isNotBlank)
            .collect(collectingAndThen(toList(), Collections::unmodifiableList));
    }
}
