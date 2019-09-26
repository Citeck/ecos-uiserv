package ru.citeck.ecos.uiserv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.config.UIServProperties;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.domain.action.dto.EvaluatorDTO;
import ru.citeck.ecos.uiserv.service.action.ActionEntityService;
import ru.citeck.ecos.uiserv.service.action.ActionService;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluatorService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * @author Roman Makarskiy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ActionServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private ActionService actionService;
    @Autowired
    private UIServProperties properties;

    @Autowired
    private ActionEntityService actionEntityService;

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

        ActionDTO createAction = getCreateNodeAction();

        ActionDTO requestAction = new ActionDTO();
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
            "        \"params\": {\n" +
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
            "        \"params\": {\n" +
            "            \"requestMethod\": \"POST\",\n" +
            "            \"context\": \"PROXY_URI\",\n" +
            "            \"url\": \"citeck/event/fire-event?eventRef=workspace://SpacesStore/fire-event\",\n" +
            "            \"confirmationMessage\": \"тест подтверждение\"\n" +
            "        },\n" +
            "        \"evaluator\": null\n" +
            "    }\n" +
            "]", JsonNode.class));

        List<ActionDTO> requiredActions = Arrays.asList(createAction, requestAction);
        List<ActionDTO> foundActions = actionService.getCardActions(record);

        assertThat(foundActions, containsInAnyOrder(requiredActions.toArray()));
    }

    @Test
    public void getJournalActionShouldBeDefaults() {
        RecordRef record = RecordRef.create("", "test-record");

        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(NullNode.getInstance());
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);

        List<ActionDTO> defaultActions = actionService.getDefaultJournalActions();
        List<ActionDTO> foundActions = actionService.getJournalActions(record, "contracts");

        assertThat(foundActions, containsInAnyOrder(defaultActions.toArray()));
    }

    @Test
    public void journalActionsFromJournalList() throws IOException {
        ActionDTO createAction = getCreateNodeAction();

        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(OBJECT_MAPPER.readValue("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"id\": \"uiserv/action@0\",\n" +
            "                \"icon\": null,\n" +
            "                \"title\": \"userActions.contract.property.approvalCancelation\",\n" +
            "                \"type\": \"CREATE_NODE\",\n" +
            "                \"params\": {\n" +
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

        List<ActionDTO> actionsFromJournal = actionService.getJournalActions(
            RecordRef.create("", "test-record"), "payments");

        assertThat(actionsFromJournal.get(0), is(createAction));
    }

    @Test
    public void journalActionsFromJournalListWithOverrideProps() throws IOException {
        String id = "default-view";
        String newIcon = "new-view-icon.png";
        String newTitle = "Some new title";

        ActionDTO overriddenAction = actionEntityService.getById(id).get();
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

        List<ActionDTO> actionFromJournal = actionService.getJournalActions(
            RecordRef.create("", "test-record"), "payments");

        assertThat(actionFromJournal.get(0), is(overriddenAction));
    }

    @Test
    public void journalActionsFromJournalListWithOverrideConfig() throws IOException {
        String id = "default-view";

        ObjectNode newConfig = OBJECT_MAPPER.createObjectNode();
        newConfig.put("size", 20);

        ActionDTO overriddenAction = actionEntityService.getById(id).get();
        overriddenAction.setConfig(newConfig);

        when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(OBJECT_MAPPER.readValue("{\n" +
            "    \"meta\": {\n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"id\": \"" + id + "\",\n" +
            "                \"params\": {\n" +
            "                    \"size\": 20\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}", JsonNode.class));
        when(recordEvaluatorService.evaluate(any(), any())).thenReturn(true);

        List<ActionDTO> actionFromJournal = actionService.getJournalActions(
            RecordRef.create("", "test-record"), "payments");

        assertThat(actionFromJournal.get(0), is(overriddenAction));
    }

    @Test
    public void journalActionsFromJournalListWithOverrideEvaluator() throws IOException {
        String id = "default-view";
        String alwaysTrueEvaluatorId = "always-true";

        ActionDTO overriddenAction = actionEntityService.getById(id).get();
        EvaluatorDTO alwaysTrue = new EvaluatorDTO();
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

        List<ActionDTO> actionFromJournal = actionService.getJournalActions(
            RecordRef.create("", "test-record"), "payments");

        assertThat(actionFromJournal.get(0), is(overriddenAction));
    }

    @Test
    public void deployDefaultActions() throws IOException {
        List<ActionDTO> defaultActions = actionService.getDefaultJournalActions();
        List<ActionDTO> actionsFromFile;

        try (InputStream inputStream = new ClassPathResource(properties.getAction().getDefaultActionsClasspath())
            .getInputStream()) {

            ActionDTO[] actions = OBJECT_MAPPER.readValue(inputStream, ActionDTO[].class);

            actionsFromFile = Arrays.asList(actions);
        }

        assertThat(defaultActions, containsInAnyOrder(actionsFromFile.toArray()));
    }

    private ActionDTO getCreateNodeAction() {
        ActionDTO createAction = new ActionDTO();
        createAction.setId("uiserv/action@0");
        createAction.setTitle("userActions.contract.property.approvalCancelation");
        createAction.setType("CREATE_NODE");

        ObjectNode createActionConfig = OBJECT_MAPPER.createObjectNode();
        createActionConfig.put("destinationAssoc", "iEvent:additionalDataItems");
        createActionConfig.put("destination", "workspace://SpacesStore/some-destination");
        createActionConfig.put("nodeType", "ctrEvent:cancelApproval");

        createAction.setConfig(createActionConfig);

        return createAction;
    }

}
