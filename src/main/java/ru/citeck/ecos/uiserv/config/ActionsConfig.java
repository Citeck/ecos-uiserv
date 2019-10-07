package ru.citeck.ecos.uiserv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.service.action.ActionEntityService;

import javax.annotation.PostConstruct;
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
    private final UIServProperties properties;

    public ActionsConfig(ActionEntityService actionEntityService, UIServProperties properties) {
        this.actionEntityService = actionEntityService;
        this.properties = properties;
    }

    @PostConstruct
    public void deployDefaultAction() {
        String classPath = properties.getAction().getDefaultActionsClasspath();
        String info = "\n======================== Deploy default action ======================\n" +
            "Classpath: " + classPath + "\n";

        try (InputStream defaultActions = new ClassPathResource(classPath)
            .getInputStream()) {

            ActionDTO[] actions = OBJECT_MAPPER.readValue(defaultActions, ActionDTO[].class);

            info += String.format("Found %s default actions", actions.length) + "\n";

            for (ActionDTO actionDTO : actions) {
                actionEntityService.create(actionDTO);
                info += "Added default action:" + actionDTO + "\n";
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed get default actions from classpath: " + classPath);
        }

        info += "====================== Default actions deployed ======================";

        log.info(info);
    }


}
