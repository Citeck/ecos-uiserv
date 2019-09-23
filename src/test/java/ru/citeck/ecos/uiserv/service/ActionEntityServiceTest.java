package ru.citeck.ecos.uiserv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.domain.action.dto.EvaluatorDTO;
import ru.citeck.ecos.uiserv.service.action.ActionEntityService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * @author Roman Makarskiy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ActionEntityServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private List<ActionDTO> actions = new ArrayList<>();

    private ActionEntityService actionEntityService;

    @Before
    public void setup() throws IOException {
        createTestActions();
    }

    @Test
    public void create() throws IOException {
        final String actionId = UUID.randomUUID().toString();

        ActionDTO actionDTO = new ActionDTO();
        actionDTO.setId(actionId);
        actionDTO.setTitle("Delete");
        actionDTO.setIcon("delete.png");
        actionDTO.setType("delete");
        actionDTO.setConfig(OBJECT_MAPPER.readValue("{\n" +
            "  \"theory\": [\n" +
            "    \"than\",\n" +
            "    true,\n" +
            "    false,\n" +
            "    [\n" +
            "      873750355.411098,\n" +
            "      888261272,\n" +
            "      -1897773026,\n" +
            "      564561522.8888073,\n" +
            "      true,\n" +
            "      \"friendly\"\n" +
            "    ],\n" +
            "    \"stepped\",\n" +
            "    false\n" +
            "  ],\n" +
            "  \"excitement\": true,\n" +
            "  \"foreign\": \"leg\",\n" +
            "  \"drop\": 1917919766,\n" +
            "  \"government\": -41507471.94034338,\n" +
            "  \"snake\": \"honor\"\n" +
            "}", JsonNode.class));


        final String evaluatorId = "has-delete-permission";

        EvaluatorDTO evaluatorDTO = new EvaluatorDTO();
        evaluatorDTO.setId(evaluatorId);
        evaluatorDTO.setConfig(OBJECT_MAPPER.readValue("{\n" +
            "  \"noted\": 1589523583,\n" +
            "  \"conversation\": false\n" +
            "}", JsonNode.class));

        actionDTO.setEvaluator(evaluatorDTO);

        actionEntityService.create(actionDTO);
        ActionDTO found = actionEntityService.getById(actionId).get();

        assertThat(found, is(actionDTO));
    }

    @Test
    public void createWithoutId() {
        ActionDTO actionDTO = new ActionDTO();
        actionDTO.setIcon("Fire");
        actionDTO.setIcon("fire.png");
        actionDTO.setType("fire");

        ActionDTO created = actionEntityService.create(actionDTO);

        assertTrue(StringUtils.isNotBlank(created.getId()));
    }

    @Test
    public void update() throws IOException {
        final String actionId = UUID.randomUUID().toString();

        ActionDTO actionDTO = new ActionDTO();
        actionDTO.setId(actionId);
        actionDTO.setTitle("Change permission");
        actionDTO.setIcon("change-permission.png");
        actionDTO.setType("change-permission");
        actionDTO.setConfig(OBJECT_MAPPER.readValue("[\n" +
            "  true,\n" +
            "  \"prevent\",\n" +
            "  false\n" +
            "]", JsonNode.class));

        EvaluatorDTO evaluatorDTO = new EvaluatorDTO();
        evaluatorDTO.setId("admin-permission");
        evaluatorDTO.setConfig(OBJECT_MAPPER.readValue("{\n" +
            "  \"syllable\": \"heat\",\n" +
            "  \"straight\": {\n" +
            "    \"press\": \"low\",\n" +
            "    \"birds\": -503651854,\n" +
            "    \"type\": -893216489.3747044\n" +
            "  },\n" +
            "  \"seems\": \"thou\"\n" +
            "}", JsonNode.class));

        actionDTO.setEvaluator(evaluatorDTO);

        actionEntityService.create(actionDTO);

        actionDTO.setIcon("new-change-permission.png");

        ActionDTO update = actionEntityService.update(actionDTO);
        ActionDTO updatedFound = actionEntityService.getById(update.getId()).get();

        assertThat(updatedFound, is(update));
    }

    @Test
    public void updateWithoutId() {
        ActionDTO actionDTO = new ActionDTO();
        actionDTO.setTitle("Copy");
        actionDTO.setType("copy");
        actionDTO.setIcon("copy.png");

        Throwable thrown = Assertions.catchThrowable(() -> actionEntityService.update(actionDTO));
        assertEquals("Cannot update entity with blank id", thrown.getMessage());
    }

    @Test
    public void createTestActionsSize() {
        assertThat(actions.size(), is(3));
    }

    @Test
    public void getById() {
        List<ActionDTO> found = actions.stream()
            .map(actionDTO -> actionEntityService.getById(actionDTO.getId()).get())
            .collect(Collectors.toList());
        assertThat(found, is(actions));
    }

    @Test
    public void getByKey() {
        Throwable thrown = Assertions.catchThrowable(() -> actionEntityService.getByKey("type", "key"));
        assertEquals("Unsupported operation", thrown.getMessage());
    }

    @Test
    public void getByKeys() {
        Throwable thrown = Assertions.catchThrowable(() -> actionEntityService.getByKeys("type",
            Arrays.asList("key1", "key2")));
        assertEquals("Unsupported operation", thrown.getMessage());
    }

    @Test
    public void delete() {
        ActionDTO actionDTO = new ActionDTO();
        actionDTO.setTitle("Change");
        actionDTO.setType("change");

        ActionDTO created = actionEntityService.create(actionDTO);
        String id = created.getId();

        actionEntityService.delete(id);

        Optional<ActionDTO> mustBeEmpty = actionEntityService.getById(id);

        assertThat(mustBeEmpty, is(Optional.empty()));
    }

    @Test
    public void deleteNotExistsEntity() {
        final String id = UUID.randomUUID().toString();
        Throwable thrown = Assertions.catchThrowable(() -> actionEntityService.delete(id));

        assertThat(thrown.getMessage(), is(String.format(
            "No class ru.citeck.ecos.uiserv.domain.action.Action entity with id %s exists!", id)));

    }

    private void createTestActions() throws IOException {
        ActionDTO fireAction = new ActionDTO();
        fireAction.setId(UUID.randomUUID().toString());
        fireAction.setTitle("Fire");
        fireAction.setIcon("fire.png");
        fireAction.setType("fire");
        fireAction.setConfig(OBJECT_MAPPER.readValue("{\n" +
            "  \"shape\": 1497189363.1737409,\n" +
            "  \"onto\": false,\n" +
            "  \"chamber\": {\n" +
            "    \"across\": 1829441809,\n" +
            "    \"bread\": true,\n" +
            "    \"screen\": true\n" +
            "  }\n" +
            "}", JsonNode.class));

        EvaluatorDTO evaluatorDTO = new EvaluatorDTO();
        evaluatorDTO.setId("has-fire-permission");
        evaluatorDTO.setConfig(OBJECT_MAPPER.readValue("[\n" +
            "  true,\n" +
            "  false,\n" +
            "  \"experiment\"\n" +
            "]", JsonNode.class));

        fireAction.setEvaluator(evaluatorDTO);

        ActionDTO printAction = new ActionDTO();
        printAction.setId(UUID.randomUUID().toString());
        printAction.setTitle("Print");
        printAction.setIcon("print.png");
        printAction.setType("print");
        printAction.setConfig(OBJECT_MAPPER.readValue("[\n" +
            "  \"behavior\",\n" +
            "  [\n" +
            "    1265789783,\n" +
            "    \"him\",\n" +
            "    \"sudden\"\n" +
            "  ],\n" +
            "  \"only\"\n" +
            "]", JsonNode.class));

        ActionDTO moveAction = new ActionDTO();
        moveAction.setId(UUID.randomUUID().toString());
        moveAction.setTitle("Move");
        moveAction.setIcon("move.png");
        moveAction.setType("move");

        EvaluatorDTO moveActionEvaluatorDTO = new EvaluatorDTO();
        moveActionEvaluatorDTO.setId("has-edit-permission");
        moveActionEvaluatorDTO.setConfig(OBJECT_MAPPER.readValue("[\n" +
            "  true,\n" +
            "  false,\n" +
            "  \"experiment\"\n" +
            "]", JsonNode.class));

        moveAction.setEvaluator(moveActionEvaluatorDTO);

        actions.add(fireAction);
        actions.add(printAction);
        actions.add(moveAction);

        actions.forEach(actionDto -> actionEntityService.create(actionDto));
    }

    @Autowired
    public void setActionEntityService(ActionEntityService actionEntityService) {
        this.actionEntityService = actionEntityService;
    }
}
