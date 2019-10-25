package ru.citeck.ecos.uiserv.web.rest.records;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.config.UiServProperties;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.uiserv.web.rest.TestEntityRecordUtil;
import ru.citeck.ecos.uiserv.web.rest.TestUtil;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
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

    /*private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String FIRE_ACTION_ID = "fire-action";
    private static final String PRINT_ACTION_ID = "print-action";
    private static final String MOVE_ACTION_ID = "move-action";
    private static final String FORE_DELETE_ACTION_ID = "for-delete-action";

    private static final String RECORD_ID_AT = "action@";

    private static final String JOURNAL_ACTION_QUERY = "{\n" +
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

    private static final String QUERY_BY_RECORD_TEMPLATE = "{\n" +
        "  \"record\": \"" + "%s" + "\",\n" +
        "  \"attributes\": {\n" +
        "    \"title\": \"title\",\n" +
        "    \"type\": \"type\",\n" +
        "    \"icon\": \"icon\",\n" +
        "    \"config\": \"config?json\",\n" +
        "    \"evaluator\": \"evaluator?json\"\n" +
        "  }\n" +
        "}";

    private MockMvc mockActionsApi;

    @Autowired
    private ActionEntityService actionEntityService;
    @Autowired
    private RestHandler restHandler;
    @Autowired
    private RecordsService recordsService;
    @Autowired
    private UiServProperties props;

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

        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(String.format(QUERY_BY_RECORD_TEMPLATE, recordId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(recordId)));
    }

    @Test
    public void getByNotExistsId() {
        String id = "some-not-exists-id";

        Throwable thrown = catchThrowable(() -> mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(String.format(QUERY_BY_RECORD_TEMPLATE, RECORD_ID_AT + id))));

        assertEquals("Records is empty", thrown.getCause().getMessage());
    }


    @Test
    public void create() throws Exception {
        String id = "action@some-test-create-action";

        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_MUTATE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content("{\n" +
                    "  \"records\": [\n" +
                    "    {\n" +
                    "      \"id\": \"" + id + "\",\n" +
                    "      \"attributes\": {\n" +
                    "        \"title\": \"Fire\",\n" +
                    "        \"type\": \"fire\",\n" +
                    "        \"icon\": \"fire.png\",\n" +
                    "        \"config\": {\n" +
                    "        \t\"color\": \"red\",\n" +
                    "        \t\"size\": 23\n" +
                    "        },\n" +
                    "        \"evaluator\": {\n" +
                    "        \t\"id\": \"has-delete-permission\",\n" +
                    "        \t\"config\": {\n" +
                    "        \t\t\"permission\": \"Delete\"\n" +
                    "        \t}\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records[0].id", is(id)))
            .andExpect(jsonPath("$.errors[*]", hasSize(0)));
    }

    @Test
    public void createWithEmptyId() throws Exception {
        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_MUTATE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content("{\n" +
                    "  \"records\": [\n" +
                    "    {\n" +
                    "      \"id\": \"" + RECORD_ID_AT + "\",\n" +
                    "      \"attributes\": {\n" +
                    "        \"title\": \"Fire\",\n" +
                    "        \"type\": \"fire\",\n" +
                    "        \"icon\": \"fire.png\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records[0].id", notNullValue()))
            .andExpect(jsonPath("$.errors[*]", hasSize(0)));
    }

    @Test
    public void delete() throws Exception {
        String id = RECORD_ID_AT + FORE_DELETE_ACTION_ID;

        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_DELETE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content("{\n" +
                    "  \"records\": [\n" +
                    "  \t\t\"" + id + "\"\n" +
                    "  \t]\n" +
                    "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records[0].id", is(id)))
            .andExpect(jsonPath("$.errors[*]", hasSize(0)));
    }

    @Test
    public void deleteNotExistsAction() {
        String id = "some-not-exists-id";

        Throwable thrown = catchThrowable(() -> mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_DELETE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content("{\n" +
                    "  \"records\": [\n" +
                    "  \t\t\"" + RECORD_ID_AT + id + "\"\n" +
                    "  \t]\n" +
                    "}")));

        assertEquals("No class ru.citeck.ecos.uiserv.domain.action.Action entity with id " + id + " exists!",
            thrown.getCause().getMessage());
    }

    //@Test
    public void queryJournalActionMustReturnDefaultActions() throws Exception {
        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(NullNode.getInstance());
        mockEvaluatorToTrue();

        List<String> defaultJournalActions = props.getAction().getDefaultJournalActions()
            .stream()
            .map(s -> RECORD_ID_AT + s)
            .collect(Collectors.toList());

        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(JOURNAL_ACTION_QUERY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount", is(defaultJournalActions.size())))
            .andExpect(jsonPath("$.records[*]", hasSize(defaultJournalActions.size())))
            .andExpect(jsonPath("$.records[*].id", containsInAnyOrder(defaultJournalActions.toArray())))
            .andExpect(jsonPath("$.errors[*]", hasSize(0)));
    }

    //@Test
    public void queryJournalActionWithOverrideInJournal() throws Exception {
        mockAlfRestToReturnJson("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"id\": \"default-download\",\n" +
            "                \"icon\": \"new-icon-download\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"default-delete\",\n" +
            "                \"type\": \"trash-remove\",\n" +
            "                \"icon\": \"icon-trash-delete\",\n" +
            "                \"title\": \"Trash delete\",\n" +
            "                \"config\": {\n" +
            "                    \"theme\": \"no-danger\"\n" +
            "                },\n" +
            "                \"evaluator\": {\n" +
            "                    \"id\": \"record-has-permission\",\n" +
            "                    \"config\": {\n" +
            "                        \"permission\": \"trash-delete\"\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}");
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);

        int recordsSize = 2;

        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(JOURNAL_ACTION_QUERY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount", is(recordsSize)))
            .andExpect(jsonPath("$.records[*]", hasSize(recordsSize)))
            .andExpect(jsonPath("$.records[0].id", is("action@default-download")))
            .andExpect(jsonPath("$.records[0].attributes.type", is("download")))
            .andExpect(jsonPath("$.records[0].attributes.icon", is("new-icon-download")))
            .andExpect(jsonPath("$.records[0].attributes.title", is("grid.inline-tools.download")))
            .andExpect(jsonPath("$.records[0].attributes.evaluator.id", is("record-has-attribute")))
            .andExpect(jsonPath("$.records[0].attributes.evaluator.config.attribute", is("_content")))

            .andExpect(jsonPath("$.records[1].id", is("action@default-delete")))
            .andExpect(jsonPath("$.records[1].attributes.type", is("trash-remove")))
            .andExpect(jsonPath("$.records[1].attributes.icon", is("icon-trash-delete")))
            .andExpect(jsonPath("$.records[1].attributes.title", is("Trash delete")))
            .andExpect(jsonPath("$.records[1].attributes.config.theme", is("no-danger")))
            .andExpect(jsonPath("$.records[1].attributes.evaluator.id", is("record-has-permission")))
            .andExpect(jsonPath("$.records[1].attributes.evaluator.config.permission", is("trash-delete")))

            .andExpect(jsonPath("$.errors[*]", hasSize(0)));
    }

    @Test
    public void queryJournalActionWithEmptyScope() throws Exception {
        mockAlfRestToReturnJson("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "        ]\n" +
            "    }\n" +
            "}");
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);


        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content("{\n" +
                    "  \"query\": {\n" +
                    "  \t\"sourceId\": \"action\",\n" +
                    "  \t\"query\": {\n" +
                    "  \t\t\"record\": \"workspace://SpacesStore/some-record\",\n" +
                    "  \t\t\"mode\": \"journal\"\n" +
                    "  \t}\n" +
                    "  },\n" +
                    "  \"attributes\": {\n" +
                    "    \"title\": \"title\",\n" +
                    "    \"type\": \"type\",\n" +
                    "    \"icon\": \"icon\",\n" +
                    "    \"config\": \"config?json\",\n" +
                    "    \"evaluator\": \"evaluator?json\"\n" +
                    "  }\n" +
                    "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].msg", is("You must specify scope, for journal mode")));
    }

    //@Test
    public void queryJournalActionsWithSpecifiedIdsInJournal() throws Exception {
        mockAlfRestToReturnJson("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"id\": \"default-view\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"" + PRINT_ACTION_ID + "\"\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}");
        mockEvaluatorToTrue();

        int recordsSize = 2;

        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(JOURNAL_ACTION_QUERY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount", is(recordsSize)))
            .andExpect(jsonPath("$.records[*]", hasSize(recordsSize)))
            .andExpect(jsonPath("$.records[0].id", is("action@default-view")))
            .andExpect(jsonPath("$.records[0].attributes.type", is("view")))
            .andExpect(jsonPath("$.records[0].attributes.icon", is("icon-on")))
            .andExpect(jsonPath("$.records[0].attributes.title", is("grid.inline-tools.show")))
            .andExpect(jsonPath("$.records[0].attributes.evaluator.id", is("record-has-permission")))
            .andExpect(jsonPath("$.records[0].attributes.evaluator.config.permission", is("Read")))

            .andExpect(jsonPath("$.records[1].id", is(RECORD_ID_AT + PRINT_ACTION_ID)))
            .andExpect(jsonPath("$.records[1].attributes.type", is("print")))
            .andExpect(jsonPath("$.records[1].attributes.icon", is("print.png")))
            .andExpect(jsonPath("$.records[1].attributes.title", is("Print")))
            .andExpect(jsonPath("$.records[1].attributes.config[1].[2]", is("sudden")))

            .andExpect(jsonPath("$.errors[*]", hasSize(0)));
    }

    @Test
    public void queryJournalActionsWithCustomActionInJournal() throws Exception {
        mockAlfRestToReturnJson("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"title\": \"Some custom action\",\n" +
            "                \"type\": \"custom-action\",\n" +
            "                \"evaluator\": {\n" +
            "                    \"id\": \"record-has-permission\",\n" +
            "                    \"config\": {\n" +
            "                        \"permission\": \"Read\"\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}");
        mockEvaluatorToTrue();

        int recordsSize = 1;

        mockActionsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(JOURNAL_ACTION_QUERY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount", is(recordsSize)))
            .andExpect(jsonPath("$.records[*]", hasSize(recordsSize)))
            .andExpect(jsonPath("$.records[0].attributes.type", is("custom-action")))
            .andExpect(jsonPath("$.records[0].attributes.title", is("Some custom action")))
            .andExpect(jsonPath("$.records[0].attributes.evaluator.id", is("record-has-permission")))
            .andExpect(jsonPath("$.records[0].attributes.evaluator.config.permission", is("Read")))
            .andExpect(jsonPath("$.errors[*]", hasSize(0)));
    }

    //TODO: write test on query card actions

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
        ActionDto fireAction = new ActionDto();
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

        EvaluatorDto EvaluatorDto = new EvaluatorDto();
        EvaluatorDto.setId("has-fire-permission");
        EvaluatorDto.setConfig(OBJECT_MAPPER.readValue("[\n" +
            "  true,\n" +
            "  false,\n" +
            "  \"experiment\"\n" +
            "]", JsonNode.class));

        fireAction.setEvaluator(EvaluatorDto);

        ActionDto printAction = new ActionDto();
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

        ActionDto moveAction = new ActionDto();
        moveAction.setId(MOVE_ACTION_ID);
        moveAction.setTitle("Move");
        moveAction.setIcon("move.png");
        moveAction.setType("move");

        EvaluatorDto moveActionEvaluatorDto = new EvaluatorDto();
        moveActionEvaluatorDto.setId("has-edit-permission");
        moveActionEvaluatorDto.setConfig(OBJECT_MAPPER.readValue("[\n" +
            "  true,\n" +
            "  false,\n" +
            "  \"experiment\"\n" +
            "]", JsonNode.class));

        moveAction.setEvaluator(moveActionEvaluatorDto);

        ActionDto forDeleteAction = new ActionDto();
        moveAction.setId(FORE_DELETE_ACTION_ID);
        moveAction.setTitle("For delete");
        moveAction.setIcon("for_delete.png");
        moveAction.setType("delete");

        actionEntityService.create(fireAction);
        actionEntityService.create(printAction);
        actionEntityService.create(moveAction);
        actionEntityService.create(forDeleteAction);
    }

    private void mockEvaluatorToTrue() {
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);
    }

    private void mockAlfRestToReturnJson(String json) throws IOException {
        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(OBJECT_MAPPER.readValue(json,
            JsonNode.class));
    }*/
}
