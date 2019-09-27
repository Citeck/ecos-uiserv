package ru.citeck.ecos.uiserv.web.rest.records;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.config.UIServProperties;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.domain.action.dto.EvaluatorDTO;
import ru.citeck.ecos.uiserv.service.action.ActionEntityService;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.uiserv.web.rest.RecordsApi;
import ru.citeck.ecos.uiserv.web.rest.TestEntityRecordUtil;
import ru.citeck.ecos.uiserv.web.rest.TestUtil;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Roman Makarskiy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ActionRecordControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String FIRE_ACTION_ID = "fire-action";
    private static final String PRINT_ACTION_ID = "print-action";
    private static final String MOVE_ACTION_ID = "move-action";

    private static final String RECORD_ID_AT = "action@";

    private MockMvc mockActionsApi;

    @Autowired
    private ActionEntityService actionEntityService;
    @Autowired
    private RestHandler restHandler;
    @Autowired
    private RecordsService recordsService;
    @Autowired
    private UIServProperties props;

    @MockBean
    @Qualifier("alfrescoRestTemplate")
    private RestTemplate restTemplate;

    @MockBean
    private RecordEvaluatorService recordEvaluatorService;

    @Before
    public void setUp() throws Exception {
        RecordsApi recordsApi = new RecordsApi(restHandler, recordsService);
        this.mockActionsApi = MockMvcBuilders.standaloneSetup(recordsApi).build();
        createTestData();
    }

    @Test
    public void getById() throws Exception {
        String recordId = RECORD_ID_AT + FIRE_ACTION_ID;

        String queryJson = "{\n" +
            "  \"record\": \"" + recordId + "\",\n" +
            "  \"attributes\": {\n" +
            "    \"title\": \"title\",\n" +
            "    \"type\": \"type\",\n" +
            "    \"icon\": \"icon\",\n" +
            "    \"config\": \"config?json\",\n" +
            "    \"evaluator\": \"evaluator?json\"\n" +
            "  }\n" +
            "}";

        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(queryJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(recordId)));
    }

    @Test
    public void queryByRefReturnDefaultJournalActions() throws Exception {
        String queryJson = "{\n" +
            "  \"query\": {\n" +
            "  \t\"sourceId\": \"action\",\n" +
            "  \t\"query\": {\n" +
            "  \t\t\"record\": \"workspace://SpacesStore/some-record\",\n" +
            "  \t\t\"mode\": \"journal\",\n" +
            "  \t\t\"scope\": \"contract-agreements\"\n" +
            "  \t}\n" +
            "  },\n" +
            "  \"attributes\": {\n" +
            "    \"title\": \"title\",\n" +
            "    \"type\": \"type\",\n" +
            "    \"icon\": \"icon\",\n" +
            "    \"config\": \"config?json\",\n" +
            "    \"evaluator\": \"evaluator?json\"\n" +
            "  }\n" +
            "}";

        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(NullNode.getInstance());
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);

        List<String> defaultJournalActions = props.getAction().getDefaultJournalActions()
            .stream()
            .map(s -> RECORD_ID_AT + s)
            .collect(Collectors.toList());


        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(queryJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records[*]", hasSize(defaultJournalActions.size())))
            .andExpect(jsonPath("$.records[*].id", containsInAnyOrder(defaultJournalActions.toArray())));
    }

    @Test
    public void checkJsonFormat() throws Exception {
        String recordId = RECORD_ID_AT + FIRE_ACTION_ID;

        String queryJson = "{\n" +
            "  \"record\": \"" + recordId + "\",\n" +
            "  \"attributes\": {\n" +
            "    \"title\": \"title\",\n" +
            "    \"type\": \"type\",\n" +
            "    \"icon\": \"icon\",\n" +
            "    \"config\": \"config?json\",\n" +
            "    \"evaluator\": \"evaluator?json\"\n" +
            "  }\n" +
            "}";

        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(queryJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(recordId)))
            .andExpect(jsonPath("$.attributes.title", is("Fire")))
            .andExpect(jsonPath("$.attributes.icon", is("fire.png")))
            .andExpect(jsonPath("$.attributes.type", is("fire")))
            .andExpect(jsonPath("$.attributes.config.shape", is("1497189363.1737409")))
            .andExpect(jsonPath("$.attributes.config.onto", is(false)))
            .andExpect(jsonPath("$.attributes.config.chamber.across", is(1829441809)))
            .andExpect(jsonPath("$.attributes.config.chamber.bread", is(true)))
            .andExpect(jsonPath("$.attributes.config.chamber.screen", is(true)))
            .andExpect(jsonPath("$.attributes.evaluator.id", is("has-fire-permission")))
            .andExpect(jsonPath("$.attributes.evaluator.config[*]", hasSize(3)))
            .andExpect(jsonPath("$.attributes.evaluator.config[0]", is(true)))
            .andExpect(jsonPath("$.attributes.evaluator.config[1]", is(false)))
            .andExpect(jsonPath("$.attributes.evaluator.config[2]", is("experiment")));
    }

    private void createTestData() throws IOException {
        ActionDTO fireAction = new ActionDTO();
        fireAction.setId(FIRE_ACTION_ID);
        fireAction.setTitle("Fire");
        fireAction.setIcon("fire.png");
        fireAction.setType("fire");
        fireAction.setConfig(OBJECT_MAPPER.readValue("{\n" +
            "  \"shape\": \"1497189363.1737409\",\n" +
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
        printAction.setId(PRINT_ACTION_ID);
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
        moveAction.setId(MOVE_ACTION_ID);
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

        actionEntityService.create(fireAction);
        actionEntityService.create(printAction);
        actionEntityService.create(moveAction);
    }
}
