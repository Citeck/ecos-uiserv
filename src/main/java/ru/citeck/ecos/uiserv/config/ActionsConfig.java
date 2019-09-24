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

    private static final String DEFAULT_ACTION_CLASSPATH = "/action/default-actions.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ActionEntityService actionEntityService;

    public ActionsConfig(ActionEntityService actionEntityService) {
        this.actionEntityService = actionEntityService;
    }

    @PostConstruct
    public void deployDefaultAction() {
        String info = "\n======================== Deploy default action ======================\n" +
            "Classpath: " + DEFAULT_ACTION_CLASSPATH + "\n";

        try (InputStream defaultActions = new ClassPathResource(DEFAULT_ACTION_CLASSPATH).getInputStream()) {

            ActionDTO[] actions = OBJECT_MAPPER.readValue(defaultActions, ActionDTO[].class);
            if (actions == null) {
                info += "No default actions found\n";
                return;
            }

            info += String.format("Found %s default actions", actions.length) + "\n";

            for (ActionDTO actionDTO : actions) {
                actionEntityService.create(actionDTO);
                info += "Added default action:" + actionDTO + "\n";
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed get default actions from classpath: " + DEFAULT_ACTION_CLASSPATH);
        }

        info += "====================== Default actions deployed ======================";

        log.info(info);
    }


}
