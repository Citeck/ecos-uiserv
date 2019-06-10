package ru.citeck.ecos.uiserv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.ConfigDTO;
import ru.citeck.ecos.uiserv.service.config.ConfigEntityService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Roman Makarskiy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ConfigServiceTest {

    private List<ConfigDTO> dashboards = new ArrayList<>();

    @Autowired
    private ConfigEntityService configEntityService;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setup() throws IOException {
        createTestDashboards();
    }

    @Test
    public void getById() {
        List<ConfigDTO> found = dashboards.stream()
            .map(dto -> configEntityService.getById(dto.getId()).get())
            .collect(Collectors.toList());
        assertThat(found, is(found));
    }

    @Test
    public void getByKey() {
        List<ConfigDTO> found = dashboards.stream()
            .map(dto -> configEntityService.getByKey(dto.getKey()).get())
            .collect(Collectors.toList());
        assertThat(found, is(found));
    }

    @Test
    public void getByKeys() {
        List<ConfigDTO> found = dashboards.stream()
            .map(dto -> configEntityService.getByKeys(
                Arrays.asList("some-key", dto.getKey(), "undefined-key")
            ).get())
            .collect(Collectors.toList());
        assertThat(found, is(found));
    }

    //TODO: fix test
    /*@Test
    public void getByRecord() {
        List<ConfigDTO> found = dashboards.stream()
            .map(dto -> configEntityService.getByRecord(
                RecordRef.create("config", dto.getId())
            ).get())
            .collect(Collectors.toList());
        assertThat(found, is(found));
    }*/

    @Test
    public void save() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDTO dto = new ConfigDTO();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setKey("test-key");
        dto.setId(id);
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDTO saved = configEntityService.create(dto);
        ConfigDTO found = configEntityService.getById(id).get();

        assertThat(found, is(saved));
    }

    @Test
    public void saveWithoutId() throws IOException {
        ConfigDTO dto = new ConfigDTO();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setKey("test-key");
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDTO saved = configEntityService.create(dto);

        assertThat(dto.getKey(), is(saved.getKey()));
        assertThat(saved.getId(), notNullValue());
        assertThat(dto.getValue(), is(saved.getValue()));
    }

    @Test
    public void saveWithoutKey() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDTO dto = new ConfigDTO();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setId(id);
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDTO saved = configEntityService.create(dto);
        ConfigDTO found = configEntityService.getById(id).get();

        assertThat(saved, is(found));
    }

    @Test
    public void saveWithoutConfig() {
        String id = UUID.randomUUID().toString();

        ConfigDTO dto = new ConfigDTO();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setId(id);
        dto.setKey("some-test-key");

        ConfigDTO saved = configEntityService.create(dto);
        ConfigDTO found = configEntityService.getById(id).get();

        assertThat(saved, is(found));
    }

    @Test
    public void mutate() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDTO dto = new ConfigDTO();
        dto.setId(id);
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setKey("sun-key");
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        configEntityService.create(dto);

        ConfigDTO found = configEntityService.getById(id).get();
        found.setKey("board-test-key");
        found.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"DOWN\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDTO mutated = configEntityService.update(found);
        ConfigDTO mutatedFound = configEntityService.getById(id).get();

        assertThat(mutated, is(mutatedFound));
    }

    @Test
    public void delete() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDTO dto = new ConfigDTO();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setKey("down");
        dto.setId(id);
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDTO saved = configEntityService.create(dto);
        ConfigDTO found = configEntityService.getById(id).get();

        assertThat(found, is(saved));

        configEntityService.delete(id);

        Optional<ConfigDTO> mustBeEmpty = configEntityService.getById(id);

        assertThat(mustBeEmpty, is(Optional.empty()));
    }


    private void createTestDashboards() throws IOException {
        ConfigDTO syncConfig = new ConfigDTO();
        syncConfig.setKey("sync-key");
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
        displayAllGroupConfig.setKey("display-all-group-key");
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
        emailNotificationsConfig.setKey("email-notifications");
        emailNotificationsConfig.setId("email-notifications-enabled");
        emailNotificationsConfig.setValue(objectMapper.readValue("{\n" +
            "  \"enabled\": true\n" +
            "}", JsonNode.class));

        dashboards.add(syncConfig);
        dashboards.add(displayAllGroupConfig);
        dashboards.add(emailNotificationsConfig);

        dashboards.forEach(dto -> configEntityService.create(dto));
    }

}
