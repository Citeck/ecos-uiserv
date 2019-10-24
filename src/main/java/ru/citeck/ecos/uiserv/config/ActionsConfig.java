package ru.citeck.ecos.uiserv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.service.action.ActionEntityService;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Roman Makarskiy
 */
@Slf4j
@Configuration
public class ActionsConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ActionEntityService actionEntityService;
    private final UiServProperties properties;

    public ActionsConfig(ActionEntityService actionEntityService, UiServProperties properties) {
        this.actionEntityService = actionEntityService;
        this.properties = properties;
    }

    //@PostConstruct
    public void deployDefaultAction() {
        String classPath = properties.getAction().getDefaultActionsClasspath();
        StringBuilder info = new StringBuilder("\n======================== Deploy default action ======================\n" +
            "Classpath: " + classPath + "\n");

        try (InputStream defaultActions = new ClassPathResource(classPath)
            .getInputStream()) {

            ActionDTO[] actions = OBJECT_MAPPER.readValue(defaultActions, ActionDTO[].class);

            info.append(String.format("Found %s default actions", actions.length)).append("\n");

            for (ActionDTO actionDTO : actions) {
                actionEntityService.create(actionDTO);
                info.append("Added default action:").append(actionDTO).append("\n");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed get default actions from classpath: " + classPath);
        }

        info.append("====================== Default actions deployed ======================");

        log.info(info.toString());
    }
}
