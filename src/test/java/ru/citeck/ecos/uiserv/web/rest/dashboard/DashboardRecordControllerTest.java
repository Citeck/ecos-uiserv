package ru.citeck.ecos.uiserv.web.rest.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;
import ru.citeck.ecos.uiserv.service.dashdoard.DashboardService;
import ru.citeck.ecos.uiserv.web.rest.RecordsApi;
import ru.citeck.ecos.uiserv.web.rest.TestUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Roman Makarskiy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DashboardRecordControllerTest {

    private MockMvc recordsMockMvc;
    private MockMvc recordsMockMvcWithRealRecordsService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RestHandler restQueryHandler;

    @Autowired
    private RecordsService recordsService;

    @Mock
    private RecordsService mockRecordsService;

    @MockBean
    private DashboardService mockDashboardService;

    @Before
    public void setup() {
        RecordsApi recordsApi = new RecordsApi(restQueryHandler, recordsService);
        this.recordsMockMvcWithRealRecordsService = MockMvcBuilders
            .standaloneSetup(recordsApi)
            .build();

        RecordsApi mockedRecordsApi = new RecordsApi(restQueryHandler, mockRecordsService);
        this.recordsMockMvc = MockMvcBuilders
            .standaloneSetup(mockedRecordsApi)
            .build();
    }

    @Test
    public void create() throws Exception {
        final String id = UUID.randomUUID().toString();
        String json = "{\n" +
            "  \"records\": [\n" +
            "  \t\t{\n" +
            "  \t\t\t\"id\": \"dashboard@\",\n" +
            "  \t\t\t\"attributes\": {\n" +
            "  \t\t\t\t\"key\": \"test-dashboard\"\n" +
            "  \t\t\t}\n" +
            "  \t\t}\n" +
            "  \t]\n" +
            "}";

        performMutateAndCheckResponseId(json, id);
    }

    @Test
    public void delete() throws Exception {
        final String id = UUID.randomUUID().toString();
        String json = "{\n" +
            "  \"records\": [\n" +
            "  \t\t\"dashboard@" + id + "\"\n" +
            "  \t]\n" +
            "}";

        performMutateAndCheckResponseId(json, id);
    }

    @Test
    public void mutate() throws Exception {
        final String id = UUID.randomUUID().toString();
        String json = "{\n" +
            "  \"records\": [\n" +
            "  \t\t{\n" +
            "  \t\t\t\"id\": \"dashboard@" + id + "\",\n" +
            "  \t\t\t\"attributes\": {\n" +
            "  \t\t\t\t\"key\": \"new-dashboard\"\n" +
            "  \t\t\t}\n" +
            "  \t\t}\n" +
            "  \t]\n" +
            "}";

        performMutateAndCheckResponseId(json, id);
    }

    private void performMutateAndCheckResponseId(String json, String id) throws Exception {
        RecordsMutResult result = new RecordsMutResult();
        RecordMeta recordMeta = new RecordMeta(id);
        result.addRecord(recordMeta);

        when(mockRecordsService.mutate(any())).thenReturn(result);

        recordsMockMvc.perform(
            post("/api/records/mutate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records[*]", hasSize(1)))
            .andExpect(jsonPath("$.records[0].id", is(id)));
    }

    @Test
    public void query() throws Exception {
        final String id = UUID.randomUUID().toString();
        String queryJson = "{\n" +
            "  \"record\": \"dashboard@" + id + "\",\n" +
            "  \"attributes\": {\n" +
            "    \"key\": \"key\",\n" +
            "    \"config\": \"config?json\"\n" +
            "  }\n" +
            "}";

        when(mockDashboardService.getById(id))
            .thenReturn(Optional.of(getTestDtoWithId(id)));

        recordsMockMvcWithRealRecordsService.perform(
            post("/api/records/query")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(queryJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is("dashboard@" + id)))
            .andExpect(jsonPath("$.attributes.key", is("main-dashboard")))
            .andExpect(jsonPath("$.attributes.config.menu.type", is("TOP")))
            .andExpect(jsonPath("$.attributes.config.layout.columns[1].widgets[0].id",
                is("some-test-widget-id")));

    }

    @Test
    public void queryNotExistsDashboard() {
        String json = "{\n" +
            "  \"record\": \"dashboard@some-test-records\",\n" +
            "  \"attributes\": {\n" +
            "    \"key\": \"key\",\n" +
            "    \"config\": \"config?json\"\n" +
            "  }\n" +
            "}";

        Throwable thrown = catchThrowable(() -> recordsMockMvcWithRealRecordsService.perform(
            post("/api/records/query")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(json)));

        assertEquals(thrown.getCause().getMessage(), "Dashboard with id some-test-records not found!");
    }

    @Test
    public void mutateNotExistsDashboard() {
        String json = "{\n" +
            "  \"records\": [\n" +
            "    {\n" +
            "      \"id\": \"dashboard@undefined-id\",\n" +
            "      \"attributes\": {\n" +
            "         \"key\": \"new-key\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        Throwable thrown = catchThrowable(() -> recordsMockMvcWithRealRecordsService.perform(
            post("/api/records/mutate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(json)));

        assertEquals(thrown.getCause().getMessage(), "Dashboard with id undefined-id not found!");
    }

    private DashboardDTO getTestDtoWithId(String id) throws IOException {
        DashboardDTO dto = new DashboardDTO();
        dto.setKey("main-dashboard");
        dto.setId(id);

        String configJson = "{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\",\n" +
            "    \"links\": [\n" +
            "      {\n" +
            "        \"label\": \"Journal\",\n" +
            "        \"position\": 0,\n" +
            "        \"link\": \"/share/page/journals\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"layout\": {\n" +
            "    \"type\": \"2-columns-big-small\",\n" +
            "    \"columns\": [\n" +
            "      {\n" +
            "        \"width\": \"60%\",\n" +
            "        \"widgets\": [\n" +
            "          {\n" +
            "            \"id\": \"a857c687-9a83-4af4-83ed-58c3c9751e04\",\n" +
            "            \"label\": \"Предпросмотр\",\n" +
            "            \"type\": \"doc-preview\",\n" +
            "            \"props\": {\n" +
            "              \"id\": \"a857c687-9a83-4af4-83ed-58c3c9751e04\",\n" +
            "              \"config\": {\n" +
            "                \"height\": \"500px\",\n" +
            "                \"link\": \"/share/proxy/alfresco/demo.pdf\",\n" +
            "                \"scale\": 1\n" +
            "              }\n" +
            "            },\n" +
            "            \"style\": {\n" +
            "              \"height\": \"300px\"\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"width\": \"40%\",\n" +
            "        \"widgets\": [\n" +
            "          {\n" +
            "            \"id\": \"some-test-widget-id\",\n" +
            "            \"label\": \"Журнал\",\n" +
            "            \"type\": \"journal\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
        JsonNode config = mapper.readValue(configJson, JsonNode.class);
        dto.setConfig(config);
        return dto;
    }

}
