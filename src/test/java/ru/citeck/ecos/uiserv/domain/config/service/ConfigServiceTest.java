package ru.citeck.ecos.uiserv.domain.config.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.app.test.config.TestConfigData;
import ru.citeck.ecos.uiserv.domain.config.dto.ConfigDto;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Roman Makarskiy
 */
@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
@ActiveProfiles(profiles = {"test-config-data", "test"})
public class ConfigServiceTest {

    @Autowired
    private ConfigEntityService configEntityService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void getById() {
        assertThat(TestConfigData.testConfigs.size(), is(3));

        List<ConfigDto> found = TestConfigData.testConfigs.stream()
            .map(dto -> configEntityService.getById(dto.getId()).orElseThrow())
            .collect(Collectors.toList());

        assertThat(found, is(found));
    }

    @Test
    public void create() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setId(id);
        dto.setValue(objectMapper.readValue("""
            {
              "menu": {
                "type": "TOP"
              }
            }""", JsonNode.class));

        ConfigDto saved = configEntityService.create(dto);
        ConfigDto found = configEntityService.getById(id).orElseThrow();

        assertThat(found, is(saved));
    }

    @Test
    public void createMultipleWithEqualsId() throws IOException {
        String id = "same-id";

        ConfigDto topConfig = new ConfigDto();
        topConfig.setTitle("Some title");
        topConfig.setDescription("Some description");
        topConfig.setId(id);
        topConfig.setValue(objectMapper.readValue("""
            {
              "menu": {
                "type": "TOP"
              }
            }""", JsonNode.class));

        ConfigDto downConfig = new ConfigDto();
        downConfig.setTitle("Some title");
        downConfig.setDescription("Some description");
        downConfig.setId(id);
        downConfig.setValue(objectMapper.readValue("""
            {
              "menu": {
                "type": "DOWN"
              }
            }""", JsonNode.class));

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
        dto.setValue(objectMapper.readValue("""
            {
              "menu": {
                "type": "TOP"
              }
            }""", JsonNode.class));

        Throwable thrown = Assertions.catchThrowable(() -> configEntityService.create(dto));

        assertEquals("'Id' attribute is mandatory for config entity", thrown.getMessage());
    }

    @Test
    public void saveIdAndValue() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setId(id);
        dto.setValue(objectMapper.readValue("""
            {
              "menu": {
                "type": "TOP"
              }
            }""", JsonNode.class));

        ConfigDto saved = configEntityService.create(dto);
        ConfigDto found = configEntityService.getById(id).orElseThrow();

        assertThat(saved, is(found));
    }

    @Test
    public void saveOnlyId() {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setId(id);

        ConfigDto saved = configEntityService.create(dto);
        ConfigDto found = configEntityService.getById(id).orElseThrow();

        assertThat(saved, is(found));
    }

    @Test
    public void mutate() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setId(id);
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setValue(objectMapper.readValue("""
            {
              "menu": {
                "type": "TOP"
              }
            }""", JsonNode.class));

        configEntityService.create(dto);

        ConfigDto found = configEntityService.getById(id).orElseThrow();
        found.setValue(objectMapper.readValue("""
            {
              "menu": {
                "type": "DOWN"
              }
            }""", JsonNode.class));

        ConfigDto mutated = configEntityService.update(found);
        ConfigDto mutatedFound = configEntityService.getById(id).orElseThrow();

        assertThat(mutated, is(mutatedFound));
    }

    @Test
    public void delete() throws IOException {
        String id = UUID.randomUUID().toString();

        ConfigDto dto = new ConfigDto();
        dto.setTitle("Some title");
        dto.setDescription("Some description");
        dto.setId(id);
        dto.setValue(objectMapper.readValue("""
            {
              "menu": {
                "type": "TOP"
              }
            }""", JsonNode.class));

        ConfigDto saved = configEntityService.create(dto);
        ConfigDto found = configEntityService.getById(id).orElseThrow();

        assertThat(found, is(saved));

        configEntityService.delete(id);

        Optional<ConfigDto> mustBeEmpty = configEntityService.getById(id);

        assertThat(mustBeEmpty, is(Optional.empty()));
    }

}
