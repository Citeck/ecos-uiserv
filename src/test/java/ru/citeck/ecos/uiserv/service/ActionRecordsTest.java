package ru.citeck.ecos.uiserv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.apps.app.module.type.type.action.ActionDto;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.service.action.ActionRecords;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Sergey Artamonov
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ActionRecordsTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private ActionRecords actionRecords;

    @MockBean
    private RecordsService recordsService;

    @Test
    public void simpleAction() throws IOException {

        when(recordsService.getMeta(any(Collection.class), any(Class.class)))
            .thenReturn(new RecordsResult<ActionRecords.RecordActionsMeta>());

        RecordsQuery query = new RecordsQuery();
        query.setQuery("" +
            "{" +
            "  \"records\": [\n" +
            "    \"workspace://SpacesStore/4cdd2b1a-5d8f-4a04-ae78-8dc454961f17\"\n" +
            "  ]\n" +
            "}");

        RecordsQueryResult<ActionRecords.RecordActions> ret = actionRecords.getMetaValues(query);
        String expected = "" +
            "{\n" +
            "  \"records\": [],\n" +
            "  \"hasMore\": false,\n" +
            "  \"totalCount\": 0\n" +
            "}";
        System.out.println(expected);
        System.out.println(ret);
        assertThat(ret.toString(), equalTo(expected));
    }

    @Test
    public void testActionFilteringOneWord() throws IOException {

        // the first action ("id": "uiserv/action@1") should be in the returned result,
        // because of it has "key": "dao.edit" and type "test-ecos-type3", which in turn
        // has "type": "record-actions" and "config": {"key": "dao.*"}
        //
        // Second action should be filtered out because of its "key": "print-form.html"
        // not matching "config": {"key": "dao.*"}
        ActionRecords.RecordActionsMeta a1 = OBJECT_MAPPER.readValue(
        "{\n" +
            "        \"type\": \"test-ecos-type3\", \n" +
            "        \"actions\": [\n" +
            "            {\n" +
            "                \"id\": \"uiserv/action@1\",\n" +
            "                \"key\": \"dao.edit\",\n" +
            "                \"name\": \"userActions.contract.property.approvalCancelation\",\n" +
            "                \"type\": \"CREATE_NODE\",\n" +
            "                \"config\": {\n" +
            "                    \"nodeType\": \"ctrEvent:cancelApproval\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"uiserv/action@2\",\n" +
            "                \"key\": \"print-form.html\",\n" +
            "                \"name\": \"userActions.contract.property.approvalApprovement\",\n" +
            "                \"type\": \"CREATE_NODE\",\n" +
            "                \"config\": {\n" +
            "                    \"nodeType\": \"ctrEvent:approveApproval\"\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "}",
            ActionRecords.RecordActionsMeta.class);

        ActionRecords.TypeActionsMeta t1 = OBJECT_MAPPER.readValue(
            "{\n" +
                "  \"type\": \"test-ecos-type\",\n" +
                "  \"actions\": []\n" +
                "}",
            ActionRecords.TypeActionsMeta.class);
        ActionRecords.TypeActionsMeta t2 = OBJECT_MAPPER.readValue("" +
                "{\n" +
                "  \"type\": \"test-ecos-type3\",\n" +
                "  \"actions\": [\n" +
                "      {\n" +
                "          \"id\": \"view\",\n" +
                "          \"name\": \"View\",\n" +
                "          \"key\": \"view-action\",\n" +
                "          \"type\": \"record-actions\",\n" +
                "          \"icon\": \"view.png\",\n" +
                "          \"order\": 2.0,\n" +
                "          \"config\": {\n" +
                "              \"key\": \"dao.*\",\n" +
                "              \"color\": \"blue\",\n" +
                "              \"size\": 10\n" +
                "          },\n" +
                "          \"evaluator\": {\n" +
                "              \"id\": \"evaluator-view\",\n" +
                "              \"config\": {\n" +
                "                  \"permission\": \"Read\"\n" +
                "              }\n" +
                "          }\n" +
                "      }\n" +
                "  ]\n" +
                "}",
            ActionRecords.TypeActionsMeta.class);



        when(recordsService.getMeta(anyCollection(), eq(ActionRecords.RecordActionsMeta.class)))
            .then(i -> new RecordsResult<ActionRecords.RecordActionsMeta>(
                Arrays.asList(a1)
            ));

        when(recordsService.getMeta(anyCollection(), eq(ActionRecords.TypeActionsMeta.class)))
            .then(invocationOnMock -> new RecordsResult<ActionRecords.TypeActionsMeta>(
                Arrays.asList(t1, t2)
            ));

        RecordsQuery query = new RecordsQuery();
        query.setQuery("" +
            "{" +
            "  \"records\": [\n" +
            "    \"workspace://SpacesStore/4cdd2b1a-5d8f-4a04-ae78-8dc454961f17\"\n" +
            "  ]\n" +
            "}");

        ActionRecords.RecordActions expectedRecord = OBJECT_MAPPER.readValue(
            "{\n" +
                "  \"id\": \"b0989faf-9c69-48c9-b455-a92214b4ea8b\",\n" +
                "  \"record\": \"workspace://SpacesStore/4cdd2b1a-5d8f-4a04-ae78-8dc454961f17\",\n" +
                "  \"actions\": [" +
                "      {\n" +
                "          \"id\": \"uiserv/action@1\",\n" +
                "          \"key\": \"dao.edit\",\n" +
                "          \"name\": \"userActions.contract.property.approvalCancelation\",\n" +
                "          \"type\": \"CREATE_NODE\",\n" +
                "          \"config\": {\n" +
                "              \"nodeType\": \"ctrEvent:cancelApproval\"\n" +
                "          }\n" +
                "      }\n" +
                "   ]\n" +
                "}\n",
            ActionRecords.RecordActions.class);
        RecordsQueryResult<ActionRecords.RecordActions> expected = new RecordsQueryResult<>(Arrays.asList(expectedRecord));
        RecordsQueryResult<ActionRecords.RecordActions> actual = actionRecords.getMetaValues(query);
        assert (actual.getRecords() != null);
        assert (actual.getRecords().get(0) != null);
        // have to manually set the constant 'id' because method 'actionRecords.getMetaValues(query)' sets it to random value
        actual.getRecords().get(0).setId("b0989faf-9c69-48c9-b455-a92214b4ea8b");
//        System.out.println(expected);
//        System.out.println(actual);
        assertThat(actual.toString(), equalTo(expected.toString()));
    }

    @Test
    public void testActionFilteringAnyWords() throws IOException {

        // the first action ("id": "uiserv/action@1") should be in the returned result,
        // because of it has "key": "dao.edit.save" and type "test-ecos-type3", which in turn
        // has "type": "record-actions" and "config": {"key": "dao.#"}
        //
        // Second action should be filtered out because of its "key": "print-form.html"
        // not matching "config": {"key": "dao.#"}
        ActionRecords.RecordActionsMeta a1 = OBJECT_MAPPER.readValue(
            "{\n" +
                "        \"type\": \"test-ecos-type3\", \n" +
                "        \"actions\": [\n" +
                "            {\n" +
                "                \"id\": \"uiserv/action@1\",\n" +
                "                \"key\": \"dao.edit.save\",\n" +
                "                \"name\": \"userActions.contract.property.approvalCancelation\",\n" +
                "                \"type\": \"CREATE_NODE\",\n" +
                "                \"config\": {\n" +
                "                    \"nodeType\": \"ctrEvent:cancelApproval\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"uiserv/action@2\",\n" +
                "                \"key\": \"print-form.html\",\n" +
                "                \"name\": \"userActions.contract.property.approvalApprovement\",\n" +
                "                \"type\": \"CREATE_NODE\",\n" +
                "                \"config\": {\n" +
                "                    \"nodeType\": \"ctrEvent:approveApproval\"\n" +
                "                }\n" +
                "            }\n" +
                "        ]\n" +
                "}",
            ActionRecords.RecordActionsMeta.class);

        ActionRecords.TypeActionsMeta t1 = OBJECT_MAPPER.readValue("" +
                "{\n" +
                "  \"type\": \"test-ecos-type3\",\n" +
                "  \"actions\": [\n" +
                "      {\n" +
                "          \"id\": \"view\",\n" +
                "          \"name\": \"View\",\n" +
                "          \"key\": \"view-action\",\n" +
                "          \"type\": \"record-actions\",\n" +
                "          \"icon\": \"view.png\",\n" +
                "          \"order\": 2.0,\n" +
                "          \"config\": {\n" +
                "              \"key\": \"dao.#\",\n" +
                "              \"color\": \"blue\",\n" +
                "              \"size\": 10\n" +
                "          },\n" +
                "          \"evaluator\": {\n" +
                "              \"id\": \"evaluator-view\",\n" +
                "              \"config\": {\n" +
                "                  \"permission\": \"Read\"\n" +
                "              }\n" +
                "          }\n" +
                "      }\n" +
                "  ]\n" +
                "}",
            ActionRecords.TypeActionsMeta.class);



        when(recordsService.getMeta(anyCollection(), eq(ActionRecords.RecordActionsMeta.class)))
            .then(i -> new RecordsResult<ActionRecords.RecordActionsMeta>(
                Arrays.asList(a1)
            ));

        when(recordsService.getMeta(anyCollection(), eq(ActionRecords.TypeActionsMeta.class)))
            .then(invocationOnMock -> new RecordsResult<ActionRecords.TypeActionsMeta>(
                Arrays.asList(t1)
            ));

        RecordsQuery query = new RecordsQuery();
        query.setQuery("" +
            "{" +
            "  \"records\": [\n" +
            "    \"workspace://SpacesStore/4cdd2b1a-5d8f-4a04-ae78-8dc454961f17\"\n" +
            "  ]\n" +
            "}");

        ActionRecords.RecordActions expectedRecord = OBJECT_MAPPER.readValue(
            "{\n" +
                "  \"id\": \"b0989faf-9c69-48c9-b455-a92214b4ea8b\",\n" +
                "  \"record\": \"workspace://SpacesStore/4cdd2b1a-5d8f-4a04-ae78-8dc454961f17\",\n" +
                "  \"actions\": [" +
                "      {\n" +
                "          \"id\": \"uiserv/action@1\",\n" +
                "          \"key\": \"dao.edit.save\",\n" +
                "          \"name\": \"userActions.contract.property.approvalCancelation\",\n" +
                "          \"type\": \"CREATE_NODE\",\n" +
                "          \"config\": {\n" +
                "              \"nodeType\": \"ctrEvent:cancelApproval\"\n" +
                "          }\n" +
                "      }\n" +
                "   ]\n" +
                "}\n",
            ActionRecords.RecordActions.class);

        RecordsQueryResult<ActionRecords.RecordActions> expected = new RecordsQueryResult<>(Arrays.asList(expectedRecord));
        RecordsQueryResult<ActionRecords.RecordActions> actual = actionRecords.getMetaValues(query);

        assert (actual.getRecords() != null);
        assert (actual.getRecords().get(0) != null);
        // have to manually set the constant 'id' because method 'actionRecords.getMetaValues(query)' sets it to random value
        actual.getRecords().get(0).setId("b0989faf-9c69-48c9-b455-a92214b4ea8b");

//        System.out.println(expected);
//        System.out.println(actual);
        assertThat(actual.toString(), equalTo(expected.toString()));
    }

    @Test
    public void testActionReplacing() throws IOException {

        // records action should be completely replaced with actions from corresponding "test-ecos-type3" type
        // because of it has "type" != "record-actions"
        ActionRecords.RecordActionsMeta a1 = OBJECT_MAPPER.readValue(
            "{\n" +
                "        \"type\": \"test-ecos-type3\", \n" +
                "        \"actions\": [\n" +
                "            {\n" +
                "                \"id\": \"uiserv/action@1\",\n" +
                "                \"key\": \"dao.edit\",\n" +
                "                \"name\": \"userActions.contract.property.approvalCancelation\",\n" +
                "                \"type\": \"CREATE_NODE\",\n" +
                "                \"config\": {\n" +
                "                    \"nodeType\": \"ctrEvent:cancelApproval\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"uiserv/action@2\",\n" +
                "                \"key\": \"print-form.html\",\n" +
                "                \"name\": \"userActions.contract.property.approvalApprovement\",\n" +
                "                \"type\": \"CREATE_NODE\",\n" +
                "                \"config\": {\n" +
                "                    \"nodeType\": \"ctrEvent:approveApproval\"\n" +
                "                }\n" +
                "            }\n" +
                "        ]\n" +
                "}",
            ActionRecords.RecordActionsMeta.class);

        ActionRecords.TypeActionsMeta t1 = OBJECT_MAPPER.readValue(
            "{\n" +
                "  \"type\": \"test-ecos-type\",\n" +
                "  \"actions\": []\n" +
                "}",
            ActionRecords.TypeActionsMeta.class);
        ActionRecords.TypeActionsMeta t2 = OBJECT_MAPPER.readValue("" +
                "{\n" +
                "  \"type\": \"test-ecos-type3\",\n" +
                "  \"actions\": [\n" +
                "      {\n" +
                "          \"id\": \"view\",\n" +
                "          \"name\": \"View\",\n" +
                "          \"key\": \"view-action\",\n" +
                "          \"type\": \"server-action\",\n" +
                "          \"icon\": \"view.png\",\n" +
                "          \"order\": 2.0,\n" +
                "          \"config\": {\n" +
                "              \"color\": \"blue\",\n" +
                "              \"size\": 10\n" +
                "          },\n" +
                "          \"evaluator\": {\n" +
                "              \"id\": \"evaluator-view\",\n" +
                "              \"config\": {\n" +
                "                  \"permission\": \"Read\"\n" +
                "              }\n" +
                "          }\n" +
                "      }\n" +
                "  ]\n" +
                "}",
            ActionRecords.TypeActionsMeta.class);



        when(recordsService.getMeta(anyCollection(), eq(ActionRecords.RecordActionsMeta.class)))
            .then(i -> new RecordsResult<ActionRecords.RecordActionsMeta>(
                Arrays.asList(a1)
            ));

        when(recordsService.getMeta(anyCollection(), eq(ActionRecords.TypeActionsMeta.class)))
            .then(invocationOnMock -> new RecordsResult<ActionRecords.TypeActionsMeta>(
                Arrays.asList(t1, t2)
            ));

        RecordsQuery query = new RecordsQuery();
        query.setQuery("" +
            "{" +
            "  \"records\": [\n" +
            "    \"workspace://SpacesStore/4cdd2b1a-5d8f-4a04-ae78-8dc454961f17\"\n" +
            "  ]\n" +
            "}");

        ActionRecords.RecordActions expectedRecord = OBJECT_MAPPER.readValue(
            "{\n" +
                "  \"id\": \"b0989faf-9c69-48c9-b455-a92214b4ea8b\",\n" +
                "  \"record\": \"workspace://SpacesStore/4cdd2b1a-5d8f-4a04-ae78-8dc454961f17\",\n" +
                "  \"actions\": [" +
                "      {\n" +
                "          \"id\": \"view\",\n" +
                "          \"name\": \"View\",\n" +
                "          \"key\": \"view-action\",\n" +
                "          \"type\": \"server-action\",\n" +
                "          \"icon\": \"view.png\",\n" +
                "          \"order\": 2.0,\n" +
                "          \"config\": {\n" +
                "              \"color\": \"blue\",\n" +
                "              \"size\": 10\n" +
                "          },\n" +
                "          \"evaluator\": {\n" +
                "              \"id\": \"evaluator-view\",\n" +
                "              \"config\": {\n" +
                "                  \"permission\": \"Read\"\n" +
                "              }\n" +
                "          }\n" +
                "      }\n" +
                "   ]\n" +
                "}\n",
            ActionRecords.RecordActions.class);

        RecordsQueryResult<ActionRecords.RecordActions> expected = new RecordsQueryResult<>(Arrays.asList(expectedRecord));
        RecordsQueryResult<ActionRecords.RecordActions> actual = actionRecords.getMetaValues(query);

        assert (actual.getRecords() != null);
        assert (actual.getRecords().get(0) != null);
        // have to manually set the constant 'id' because method 'actionRecords.getMetaValues(query)' sets it to random value
        actual.getRecords().get(0).setId("b0989faf-9c69-48c9-b455-a92214b4ea8b");

//        System.out.println(expected);
//        System.out.println(actual);
        assertThat(actual.toString(), equalTo(expected.toString()));
    }

    @Test
    public void testActionCopying() throws IOException {

        // both actions of the source recordRef should be in the returned result,
        // because it's type "test-ecos-type3" has "type": "record-actions" and no "key in "config"
        ActionRecords.RecordActionsMeta a1 = OBJECT_MAPPER.readValue(
            "{\n" +
                "        \"type\": \"test-ecos-type3\", \n" +
                "        \"actions\": [\n" +
                "            {\n" +
                "                \"id\": \"uiserv/action@1\",\n" +
                "                \"key\": \"dao.edit\",\n" +
                "                \"name\": \"userActions.contract.property.approvalCancelation\",\n" +
                "                \"type\": \"CREATE_NODE\",\n" +
                "                \"config\": {\n" +
                "                    \"nodeType\": \"ctrEvent:cancelApproval\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"uiserv/action@2\",\n" +
                "                \"key\": \"print-form.html\",\n" +
                "                \"name\": \"userActions.contract.property.approvalApprovement\",\n" +
                "                \"type\": \"CREATE_NODE\",\n" +
                "                \"config\": {\n" +
                "                    \"nodeType\": \"ctrEvent:approveApproval\"\n" +
                "                }\n" +
                "            }\n" +
                "        ]\n" +
                "}",
            ActionRecords.RecordActionsMeta.class);

        ActionRecords.TypeActionsMeta t1 = OBJECT_MAPPER.readValue(
            "{\n" +
                "  \"type\": \"test-ecos-type\",\n" +
                "  \"actions\": []\n" +
                "}",
            ActionRecords.TypeActionsMeta.class);
        ActionRecords.TypeActionsMeta t2 = OBJECT_MAPPER.readValue("" +
                "{\n" +
                "  \"type\": \"test-ecos-type3\",\n" +
                "  \"actions\": [\n" +
                "      {\n" +
                "          \"id\": \"view\",\n" +
                "          \"name\": \"View\",\n" +
                "          \"key\": \"view-action\",\n" +
                "          \"type\": \"record-actions\",\n" +
                "          \"icon\": \"view.png\",\n" +
                "          \"order\": 2.0,\n" +
                "          \"config\": {\n" +
                "              \"color\": \"blue\",\n" +
                "              \"size\": 10\n" +
                "          },\n" +
                "          \"evaluator\": {\n" +
                "              \"id\": \"evaluator-view\",\n" +
                "              \"config\": {\n" +
                "                  \"permission\": \"Read\"\n" +
                "              }\n" +
                "          }\n" +
                "      }\n" +
                "  ]\n" +
                "}",
            ActionRecords.TypeActionsMeta.class);



        when(recordsService.getMeta(anyCollection(), eq(ActionRecords.RecordActionsMeta.class)))
            .then(i -> new RecordsResult<ActionRecords.RecordActionsMeta>(
                Arrays.asList(a1)
            ));

        when(recordsService.getMeta(anyCollection(), eq(ActionRecords.TypeActionsMeta.class)))
            .then(invocationOnMock -> new RecordsResult<ActionRecords.TypeActionsMeta>(
                Arrays.asList(t1, t2)
            ));

        RecordsQuery query = new RecordsQuery();
        query.setQuery("" +
            "{" +
            "  \"records\": [\n" +
            "    \"workspace://SpacesStore/4cdd2b1a-5d8f-4a04-ae78-8dc454961f17\"\n" +
            "  ]\n" +
            "}");

        ActionRecords.RecordActions expectedRecord = OBJECT_MAPPER.readValue(
            "{\n" +
                "  \"id\": \"b0989faf-9c69-48c9-b455-a92214b4ea8b\",\n" +
                "  \"record\": \"workspace://SpacesStore/4cdd2b1a-5d8f-4a04-ae78-8dc454961f17\",\n" +
                "  \"actions\": [" +
                "            {\n" +
                "                \"id\": \"uiserv/action@1\",\n" +
                "                \"key\": \"dao.edit\",\n" +
                "                \"name\": \"userActions.contract.property.approvalCancelation\",\n" +
                "                \"type\": \"CREATE_NODE\",\n" +
                "                \"config\": {\n" +
                "                    \"nodeType\": \"ctrEvent:cancelApproval\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"uiserv/action@2\",\n" +
                "                \"key\": \"print-form.html\",\n" +
                "                \"name\": \"userActions.contract.property.approvalApprovement\",\n" +
                "                \"type\": \"CREATE_NODE\",\n" +
                "                \"config\": {\n" +
                "                    \"nodeType\": \"ctrEvent:approveApproval\"\n" +
                "                }\n" +
                "            }\n" +
                "   ]\n" +
                "}\n",
            ActionRecords.RecordActions.class);

        RecordsQueryResult<ActionRecords.RecordActions> expected = new RecordsQueryResult<>(Arrays.asList(expectedRecord));
        RecordsQueryResult<ActionRecords.RecordActions> actual = actionRecords.getMetaValues(query);

        assert (actual.getRecords() != null);
        assert (actual.getRecords().get(0) != null);
        // have to manually set the constant 'id' because method 'actionRecords.getMetaValues(query)' sets it to random value
        actual.getRecords().get(0).setId("b0989faf-9c69-48c9-b455-a92214b4ea8b");

//        System.out.println(expected);
//        System.out.println(actual);
        assertThat(actual.toString(), equalTo(expected.toString()));
    }

}
