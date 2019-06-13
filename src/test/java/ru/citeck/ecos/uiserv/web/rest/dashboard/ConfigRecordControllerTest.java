package ru.citeck.ecos.uiserv.web.rest.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.ConfigDTO;
import ru.citeck.ecos.uiserv.service.config.ConfigEntityService;
import ru.citeck.ecos.uiserv.web.rest.RecordsApi;
import ru.citeck.ecos.uiserv.web.rest.TestEntityRecordUtil;
import ru.citeck.ecos.uiserv.web.rest.TestUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Roman Makarskiy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ConfigRecordControllerTest {

    private static final String RECORD_ID = "config";
    private static final String RECORD_ID_AT = RECORD_ID + "@";

    private MockMvc mockRecordsApi;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RestHandler restQueryHandler;

    @Autowired
    private RecordsService recordsService;

    @MockBean
    private ConfigEntityService configEntityService;

    @Before
    public void setup() {
        RecordsApi recordsApi = new RecordsApi(restQueryHandler, recordsService);
        this.mockRecordsApi = MockMvcBuilders
            .standaloneSetup(recordsApi)
            .build();
    }

    @Test
    public void query() throws Exception {
        final String id = "some-test-config-with-large-json-value";
        String queryJson = "{\n" +
            "  \"record\": \"" + RECORD_ID_AT + id + "\",\n" +
            "  \"attributes\": {\n" +
            "    \"title\": \"title\",\n" +
            "    \"description\": \"description\",\n" +
            "    \"value\": \"value?json\"\n" +
            "  }\n" +
            "}";

        when(configEntityService.getById(id))
            .thenReturn(Optional.of(getTestDtoForQueryWithId(id)));

        mockRecordsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(queryJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(RECORD_ID_AT + id)))
            .andExpect(jsonPath("$.attributes.title", is("test-config-title")))
            .andExpect(jsonPath("$.attributes.description", is("test-config-description")))
            .andExpect(jsonPath("$.attributes.value.type", is("TOP")))

            .andExpect(jsonPath("$.attributes.value.links[*]", hasSize(3)))

            .andExpect(jsonPath("$.attributes.value.links[0].label", is("Журнал")))
            .andExpect(jsonPath("$.attributes.value.links[0].position", is(0)))
            .andExpect(jsonPath("$.attributes.value.links[0].link", is("/share/page/journals")))

            .andExpect(jsonPath("$.attributes.value.links[1].label", is("Журнал дашборда и еще " +
                "много-много текста в этой ссылке")))
            .andExpect(jsonPath("$.attributes.value.links[1].position", is(1)))
            .andExpect(jsonPath("$.attributes.value.links[1].link", is("/share/page/journalsDashboard")))

            .andExpect(jsonPath("$.attributes.value.links[2].label", is("Настройка дашборда")))
            .andExpect(jsonPath("$.attributes.value.links[2].position", is(2)))
            .andExpect(jsonPath("$.attributes.value.links[2].link", is("/dashboard/settings")));
    }

    @Test
    public void create() throws Exception {
        final String id = UUID.randomUUID().toString();
        String title = "Menu config";
        String description = "Some description for menu config";

        String json = "{\n" +
            "  \"records\": [\n" +
            "    {\n" +
            "      \"id\": \"" + RECORD_ID_AT + id + "\",\n" +
            "      \"attributes\": {\n" +
            "        \"title\": \"" + title + "\",\n" +
            "        \"description\": \"" + description + "\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        ConfigDTO dto = new ConfigDTO();
        dto.setId(id);
        dto.setTitle(title);
        dto.setDescription(description);

        ConfigDTO createdDto = new ConfigDTO();
        createdDto.setId(id);
        createdDto.setTitle(title);
        createdDto.setDescription(description);

        when(configEntityService.create(dto)).thenReturn(createdDto);

        TestEntityRecordUtil.performMutateAndCheckResponseId(json, RECORD_ID_AT + id, mockRecordsApi);
    }

    @Test
    public void mutate() throws Exception {
        final String id = UUID.randomUUID().toString();
        String title = "new-title-key";

        String json = "{\n" +
            "  \"records\": [\n" +
            "  \t\t{\n" +
            "  \t\t\t\"id\": \"" + RECORD_ID_AT + id + "\",\n" +
            "  \t\t\t\"attributes\": {\n" +
            "  \t\t\t\t\"title\": \"" + title + "\"\n" +
            "  \t\t\t}\n" +
            "  \t\t}\n" +
            "  \t]\n" +
            "}";

        ConfigDTO dto = new ConfigDTO();
        dto.setId(id);

        ConfigDTO createdDto = new ConfigDTO();
        createdDto.setId(id);

        when(configEntityService.getById(id)).thenReturn(Optional.of(dto));
        when(configEntityService.update(dto)).thenReturn(createdDto);

        TestEntityRecordUtil.performMutateAndCheckResponseId(json, RECORD_ID_AT + id, mockRecordsApi);
    }

    @Test
    public void delete() throws Exception {
        final String id = UUID.randomUUID().toString();
        String json = "{\n" +
            "  \"records\": [\n" +
            "  \t\t\"" + RECORD_ID_AT + id + "\"\n" +
            "  \t]\n" +
            "}";

        mockRecordsApi.perform(
            MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_DELETE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records[*]", hasSize(1)))
            .andExpect(jsonPath("$.records[0].id", is(RECORD_ID_AT + id)));
    }

    @Test
    public void queryNotExistsConfig() {
        String nonExistsId = "on-exists-record-id";
        String json = "{\n" +
            "  \"record\": \"" + RECORD_ID_AT + nonExistsId + "\",\n" +
            "  \"attributes\": {\n" +
            "    \"key\": \"key\",\n" +
            "    \"config\": \"config?json\"\n" +
            "  }\n" +
            "}";

        Throwable thrown = catchThrowable(() -> mockRecordsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(json)));

        assertEquals("Entity with id <" + nonExistsId + "> not found!", thrown.getCause().getMessage());
    }

    @Test
    public void createWithoutId() {
        String json = "{\n" +
            "  \"records\": [\n" +
            "    {\n" +
            "      \"id\": \"" + RECORD_ID_AT + "\",\n" +
            "      \"attributes\": {\n" +
            "         \"title\": \"some-title\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        Throwable thrown = catchThrowable(() -> mockRecordsApi.perform(
            post(TestEntityRecordUtil.URL_RECORDS_MUTATE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(json)));

        assertEquals("Parameter 'id' is mandatory for config record", thrown.getCause().getMessage());
    }

    private ConfigDTO getTestDtoForQueryWithId(String id) throws IOException {
        ConfigDTO dto = new ConfigDTO();
        dto.setTitle("test-config-title");
        dto.setDescription("test-config-description");
        dto.setId(id);

        String configJson = "{\n" +
            "          \"type\": \"TOP\",\n" +
            "          \"links\": [\n" +
            "            {\n" +
            "              \"label\": \"Журнал\",\n" +
            "              \"position\": 0,\n" +
            "              \"link\": \"/share/page/journals\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"label\": \"Журнал дашборда и еще много-много текста в этой ссылке\",\n" +
            "              \"position\": 1,\n" +
            "              \"link\": \"/share/page/journalsDashboard\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"label\": \"Настройка дашборда\",\n" +
            "              \"position\": 2,\n" +
            "              \"link\": \"/dashboard/settings\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }";
        JsonNode config = mapper.readValue(configJson, JsonNode.class);
        dto.setValue(config);
        return dto;
    }

}
