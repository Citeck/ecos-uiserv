package ru.citeck.ecos.uiserv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * @author Roman Makarskiy
 */
@Slf4j
@Configuration
public class ActionsConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    //@PostConstruct
    /*public void deployDefaultAction() {
        String classPath = properties.getAction().getDefaultActionsClasspath();
        StringBuilder info = new StringBuilder("\n======================== Deploy default action ======================\n" +
            "Classpath: " + classPath + "\n");

        try (InputStream defaultActions = new ClassPathResource(classPath)
            .getInputStream()) {

            ActionDto[] actions = OBJECT_MAPPER.readValue(defaultActions, ActionDto[].class);

            info.append(String.format("Found %s default actions", actions.length)).append("\n");

            for (ActionDto ActionDto : actions) {
                actionEntityService.create(ActionDto);
                info.append("Added default action:").append(ActionDto).append("\n");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed get default actions from classpath: " + classPath);
        }

        info.append("====================== Default actions deployed ======================");

        log.info(info.toString());
    }*/
}
