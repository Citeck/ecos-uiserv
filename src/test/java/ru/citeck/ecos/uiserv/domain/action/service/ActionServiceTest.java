package ru.citeck.ecos.uiserv.domain.action.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.app.application.props.UiServProperties;

import java.io.IOException;

/**
 * @author Roman Makarskiy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ActionServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

//    @Autowired
//    private ActionService actionService;
    @Autowired
    private UiServProperties properties;

    @MockBean
    @Qualifier("alfrescoRestTemplate")
    private RestTemplate restTemplate;

    @MockBean
    private RecordsService recordsService;

    @MockBean
    private RecordEvaluatorService recordEvaluatorService;

    @Test
    public void getCardActions() throws IOException {
        RecordRef record = RecordRef.create("", "test-card-action-record");

        /*ActionDto createAction = getCreateNodeAction();

        ActionDto requestAction = new ActionDto();
        requestAction.setId("uiserv/action@1");
        requestAction.setTitle("Тестовое действие");
        requestAction.setType("userActions.contract.property.approvalCancelation");
        requestAction.setType("REQUEST");

        ObjectNode requestActionConfig = OBJECT_MAPPER.createObjectNode();
        requestActionConfig.put("requestMethod", "POST");
        requestActionConfig.put("context", "PROXY_URI");
        requestActionConfig.put("url", "citeck/event/fire-event?eventRef=workspace://SpacesStore/fire-event");
        requestActionConfig.put("confirmationMessage", "тест подтверждение");

        requestAction.setConfig(requestActionConfig);

        when(recordsService.getAttribute(any(RecordRef.class), anyString())).thenReturn(OBJECT_MAPPER.readValue("[\n" +
            "    {\n" +
            "        \"id\": \"uiserv/action@0\",\n" +
            "        \"icon\": null,\n" +
            "        \"title\": \"userActions.contract.property.approvalCancelation\",\n" +
            "        \"type\": \"CREATE_NODE\",\n" +
            "        \"config\": {\n" +
            "            \"destinationAssoc\": \"iEvent:additionalDataItems\",\n" +
            "            \"destination\": \"workspace://SpacesStore/some-destination\",\n" +
            "            \"nodeType\": \"ctrEvent:cancelApproval\"\n" +
            "        },\n" +
            "        \"evaluator\": null\n" +
            "    },\n" +
            "    {\n" +
            "        \"id\": \"uiserv/action@1\",\n" +
            "        \"icon\": null,\n" +
            "        \"title\": \"Тестовое действие\",\n" +
            "        \"type\": \"REQUEST\",\n" +
            "        \"config\": {\n" +
            "            \"requestMethod\": \"POST\",\n" +
            "            \"context\": \"PROXY_URI\",\n" +
            "            \"url\": \"citeck/event/fire-event?eventRef=workspace://SpacesStore/fire-event\",\n" +
            "            \"confirmationMessage\": \"тест подтверждение\"\n" +
            "        },\n" +
            "        \"evaluator\": null\n" +
            "    }\n" +
            "]", JsonNode.class));

        List<ActionDto> requiredActions = Arrays.asList(createAction, requestAction);
        List<ActionDto> foundActions = actionService.getCardActions(record);

        assertThat(foundActions, containsInAnyOrder(requiredActions.toArray()));*/
    }

    @Test
    public void getJournalActionShouldBeDefaults() {
        /*RecordRef record = RecordRef.create("", "test-record");

        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(NullNode.getInstance());
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);

        List<ActionDto> defaultActions = actionService.getDefaultJournalActions();
        List<ActionDto> foundActions = actionService.getJournalActions(record, "contracts");

        assertThat(foundActions, containsInAnyOrder(defaultActions.toArray()));*/
    }

    @Test
    public void journalActionsFromJournalList() throws IOException {
        /*ActionDto createAction = getCreateNodeAction();

        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(OBJECT_MAPPER.readValue("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"id\": \"uiserv/action@0\",\n" +
            "                \"icon\": null,\n" +
            "                \"title\": \"userActions.contract.property.approvalCancelation\",\n" +
            "                \"type\": \"CREATE_NODE\",\n" +
            "                \"config\": {\n" +
            "                    \"destinationAssoc\": \"iEvent:additionalDataItems\",\n" +
            "                    \"destination\": \"workspace://SpacesStore/some-destination\",\n" +
            "                    \"nodeType\": \"ctrEvent:cancelApproval\"\n" +
            "                },\n" +
            "                \"evaluator\": null\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}", JsonNode.class));
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);

        List<ActionDto> actionsFromJournal = actionService.getJournalActions(
            RecordRef.create("", "test-record"), "payments");

        assertThat(actionsFromJournal.get(0), is(createAction));*/
    }

    //@Test
    public void journalActionsFromJournalListWithOverrideProps() throws IOException {
        /*String id = "default-view";
        String newIcon = "new-view-icon.png";
        String newTitle = "Some new title";

        ActionDto overriddenAction = actionEntityService.getById(id).get();
        overriddenAction.setIcon(newIcon);
        overriddenAction.setTitle(newTitle);

        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(OBJECT_MAPPER.readValue("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"id\": \"" + id + "\",\n" +
            "                \"icon\": \"" + newIcon + "\",\n" +
            "                \"title\": \"" + newTitle + "\"\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}", JsonNode.class));
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);

        List<ActionDto> actionFromJournal = actionService.getJournalActions(
            RecordRef.create("", "test-record"), "payments");

        assertThat(actionFromJournal.get(0), is(overriddenAction));*/
    }

    //@Test
    public void journalActionsFromJournalListWithOverrideConfig() throws IOException {
        /*String id = "default-view";

        ObjectNode newConfig = OBJECT_MAPPER.createObjectNode();
        newConfig.put("size", 20);

        ActionDto overriddenAction = actionEntityService.getById(id).get();
        overriddenAction.setConfig(newConfig);

        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(OBJECT_MAPPER.readValue("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"id\": \"" + id + "\",\n" +
            "                \"config\": {\n" +
            "                    \"size\": 20\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}", JsonNode.class));
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);

        List<ActionDto> actionFromJournal = actionService.getJournalActions(
            RecordRef.create("", "test-record"), "payments");

        assertThat(actionFromJournal.get(0), is(overriddenAction));*/
    }

    //@Test
    public void journalActionsFromJournalListWithOverrideEvaluator() throws IOException {
        /*String id = "default-view";
        String alwaysTrueEvaluatorId = "always-true";

        ActionDto overriddenAction = actionEntityService.getById(id).get();
        EvaluatorDto alwaysTrue = new EvaluatorDto();
        alwaysTrue.setId(alwaysTrueEvaluatorId);

        overriddenAction.setEvaluator(alwaysTrue);

        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(OBJECT_MAPPER.readValue("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"id\": \"" + id + "\",\n" +
            "                \"evaluator\": {\n" +
            "                    \"id\": \"" + alwaysTrueEvaluatorId + "\"\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}", JsonNode.class));
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);

        List<ActionDto> actionFromJournal = actionService.getJournalActions(
            RecordRef.create("", "test-record"), "payments");

        assertThat(actionFromJournal.get(0), is(overriddenAction));*/
    }

    //@Test
    public void deployDefaultActions() throws IOException {
        /*List<ActionDto> defaultActions = actionService.getDefaultJournalActions();
        List<ActionDto> actionsFromFile;

        try (InputStream inputStream = new ClassPathResource(properties.getAction().getDefaultActionsClasspath())
            .getInputStream()) {

            ActionDto[] actions = OBJECT_MAPPER.readValue(inputStream, ActionDto[].class);

            actionsFromFile = Arrays.asList(actions);
        }

        assertThat(defaultActions, containsInAnyOrder(actionsFromFile.toArray()));*/
    }

   /* private ActionDto getCreateNodeAction() {
        ActionDto createAction = new ActionDto();
        createAction.setId("uiserv/action@0");
        createAction.setTitle("userActions.contract.property.approvalCancelation");
        createAction.setType("CREATE_NODE");

        ObjectNode createActionConfig = OBJECT_MAPPER.createObjectNode();
        createActionConfig.put("destinationAssoc", "iEvent:additionalDataItems");
        createActionConfig.put("destination", "workspace://SpacesStore/some-destination");
        createActionConfig.put("nodeType", "ctrEvent:cancelApproval");

        createAction.setConfig(createActionConfig);

        return createAction;
    }*/

}
