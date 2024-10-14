package ru.citeck.ecos.uiserv.domain.dashboard.api.records;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto;
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Roman Makarskiy
 */
@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
public class DashboardRecordControllerTest {

    private static final String RECORD_ID = "dashboard";
    private static final String RECORD_ID_AT = RECORD_ID + "@";

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RecordsService recordsService;

    @Autowired
    private DashboardService dashboardService;

    @BeforeEach
    public void setup() {
        dashboardService.getAllDashboards().forEach(d -> dashboardService.removeDashboard(d.getId()));
    }

    @Test
    public void query() throws Exception {

        final String id = UUID.randomUUID().toString();

        recordsService.mutate(RECORD_ID_AT, getTestDtoForQueryWithId(id));

        val atts = recordsService.getAtts(RECORD_ID_AT + id, Map.of(
            "key", "key",
            "config", "config?json"
        ));

        assertThat(atts.get("$.config.menu.type").asText()).isEqualTo("TOP");
        assertThat(atts.get("$.config.layout.columns[1].widgets[0].id").asText())
            .isEqualTo("some-test-widget-id");
    }

    @Test
    public void create() throws Exception {
        final String id = UUID.randomUUID().toString();
        String key = "test-dashboard";
        val res = recordsService.mutate(RECORD_ID_AT, Map.of(
            "id", id,
            "key", key
        ));
        assertThat(res.getLocalId()).isEqualTo(id);
    }

    @Test
    public void delete() throws Exception {

        final String id = UUID.randomUUID().toString();

        String key = "test-dashboard";
        recordsService.mutate(RECORD_ID_AT, Map.of(
            "id", id,
            "key", key
        ));
        assertThat(recordsService.getAtt(RECORD_ID_AT + id, "_notExists?bool").asBoolean()).isFalse();
        recordsService.delete(RECORD_ID_AT + id);
        assertThat(recordsService.getAtt(RECORD_ID_AT + id, "_notExists?bool").asBoolean()).isTrue();
    }

    @Test
    public void mutateNotExistsDashboard() {

        String nonExistsId = "some-non-exists-id";

        val exception = assertThrows(RuntimeException.class, () -> {
                recordsService.mutate(
                    RECORD_ID_AT + nonExistsId,
                    Map.of("key", "new-key")
                );
            }
        );
        assertEquals("Dashboard with id '" + nonExistsId + "' is not found!", exception.getMessage());
    }

    private DashboardDto getTestDtoForQueryWithId(String id) throws IOException {
        DashboardDto.Builder dto = DashboardDto.create();
        dto.setTypeRef(EntityRef.create("emodel", "type", "main-dashboard"));
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
        dto.withConfig(Json.getMapper().convert(config, ObjectData.class));
        return dto.build();
    }

}
