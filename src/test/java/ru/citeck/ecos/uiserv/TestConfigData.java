package ru.citeck.ecos.uiserv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.citeck.ecos.uiserv.domain.config.dto.ConfigDto;
import ru.citeck.ecos.uiserv.domain.config.service.ConfigEntityService;

import java.util.ArrayList;
import java.util.List;

@Profile({"test-config-data"})
@Configuration
public class TestConfigData {

    public static List<ConfigDto> testConfigs =  new ArrayList<>();

    @Bean
    public CommandLineRunner dataLoader(ConfigEntityService configEntityService, ObjectMapper objectMapper) {
        return args -> {
            ConfigDto syncConfig = new ConfigDto();
            syncConfig.setId("sync-id");
            syncConfig.setTitle("Sync config");
            syncConfig.setDescription("Global sync settings");
            syncConfig.setValue(objectMapper.readValue("{\n" +
                "  \"enabled\": true,\n" +
                "  \"cron\": \"0,3,12 0 0 ? * * *\"\n" +
                "}", JsonNode.class));

            ConfigDto displayAllGroupConfig = new ConfigDto();
            displayAllGroupConfig.setTitle("Display all group config");
            displayAllGroupConfig.setDescription("Users and groups that will be shown to the group");
            displayAllGroupConfig.setId("display-all-group-id");
            displayAllGroupConfig.setValue(objectMapper.readValue("{\n" +
                "  \"users\": [\n" +
                "    {\n" +
                "      \"user\": \"admin\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"user\": \"vladko\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"groups\": [\n" +
                "    {\n" +
                "      \"group\": \"ADMINISTRATORS\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"group\": \"contract-services\"\n" +
                "    }\n" +
                "  ]\n" +
                "}", JsonNode.class));

            ConfigDto emailNotificationsConfig = new ConfigDto();
            emailNotificationsConfig.setId("email-notifications-enabled");
            emailNotificationsConfig.setValue(objectMapper.readValue("{\n" +
                "  \"enabled\": true\n" +
                "}", JsonNode.class));

            testConfigs.add(syncConfig);
            testConfigs.add(displayAllGroupConfig);
            testConfigs.add(emailNotificationsConfig);

            testConfigs.forEach(configEntityService::create);
        };
    }

}
