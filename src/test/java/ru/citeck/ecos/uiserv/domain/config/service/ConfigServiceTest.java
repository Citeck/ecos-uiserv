package ru.citeck.ecos.uiserv.domain.config.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.app.test.config.TestConfigData;
import ru.citeck.ecos.uiserv.domain.config.dto.ConfigDto;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Roman Makarskiy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@ActiveProfiles(profiles = "test-config-data")
public class ConfigServiceTest {

    @Autowired
    private ConfigEntityService configEntityService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void getById() {
        assertThat(TestConfigData.testConfigs.size(), is(3));

        List<ConfigDto> found = TestConfigData.testConfigs.stream()
            .map(dto -> configEntityService.getById(dto.getId()).get())
            .collect(Collectors.toList());

        assertThat(found, is(found));
    }

    //TODO: fix test
    /*@Test
    public void getByRecord() {
        List<ConfigDTO> found = configs.stream()
            .map(dto -> configEntityService.getByRecord(
                RecordRef.create("config", dto.getId())
            ).get())
            .collect(Collectors.toList());
        assertThat(found, is(found));
    }*/

    @Test
    public void create() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setId(id);
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDto saved = configEntityService.create(dto);
        ConfigDto found = configEntityService.getById(id).get();

        assertThat(found, is(saved));
    }

    @Test
    public void createMultipleWithEqualsId() throws IOException {
        String id = "same-id";

        ConfigDto topConfig = new ConfigDto();
        topConfig.setTitle("Some title");
        topConfig.setDescription("Some description");
        topConfig.setId(id);
        topConfig.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDto downConfig = new ConfigDto();
        downConfig.setTitle("Some title");
        downConfig.setDescription("Some description");
        downConfig.setId(id);
        downConfig.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"DOWN\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        Throwable thrown = Assertions.catchThrowable(() -> {
            configEntityService.create(topConfig);
            configEntityService.create(downConfig);
        });

        assertEquals(String.format("Config with id <%s> already exists, use update instead", id), thrown.getMessage());
    }

    @Test
    public void saveWithoutId() throws IOException {
        ConfigDto dto = new ConfigDto();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        Throwable thrown = Assertions.catchThrowable(() -> configEntityService.create(dto));

        assertEquals("'Id' attribute is mandatory for config entity", thrown.getMessage());
    }

    @Test
    public void saveIdAndValue() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setId(id);
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDto saved = configEntityService.create(dto);
        ConfigDto found = configEntityService.getById(id).get();

        assertThat(saved, is(found));
    }

    @Test
    public void saveOnlyId() {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setId(id);

        ConfigDto saved = configEntityService.create(dto);
        ConfigDto found = configEntityService.getById(id).get();

        assertThat(saved, is(found));
    }

    @Test
    public void mutate() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setId(id);
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        configEntityService.create(dto);

        ConfigDto found = configEntityService.getById(id).get();
        found.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"DOWN\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDto mutated = configEntityService.update(found);
        ConfigDto mutatedFound = configEntityService.getById(id).get();

        assertThat(mutated, is(mutatedFound));
    }

    @Test
    public void delete() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setId(id);
        dto.setValue(objectMapper.readValue("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", JsonNode.class));

        ConfigDto saved = configEntityService.create(dto);
        ConfigDto found = configEntityService.getById(id).get();

        assertThat(found, is(saved));

        configEntityService.delete(id);

        Optional<ConfigDto> mustBeEmpty = configEntityService.getById(id);

        assertThat(mustBeEmpty, is(Optional.empty()));
    }

}
