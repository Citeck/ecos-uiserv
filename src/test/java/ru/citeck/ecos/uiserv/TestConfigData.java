package ru.citeck.ecos.uiserv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.citeck.ecos.uiserv.domain.ConfigDTO;
import ru.citeck.ecos.uiserv.service.config.ConfigEntityService;

import java.util.ArrayList;
import java.util.List;

@Profile({"test-config-data"})
@Configuration
public class TestConfigData {

    public static List<ConfigDTO> testConfigs =  new ArrayList<>();

    @Bean
    public CommandLineRunner dataLoader(ConfigEntityService configEntityService, ObjectMapper objectMapper) {
        return args -> {
            ConfigDTO syncConfig = new ConfigDTO();
            syncConfig.setId("sync-id");
            syncConfig.setTitle("Sync config");
            syncConfig.setDescription("Global sync settings");
            syncConfig.setValue(objectMapper.readValue("{\n" +
                "  \"enabled\": true,\n" +
                "  \"cron\": \"0,3,12 0 0 ? * * *\"\n" +
                "}", JsonNode.class));

            ConfigDTO displayAllGroupConfig = new ConfigDTO();
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

            ConfigDTO emailNotificationsConfig = new ConfigDTO();
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
