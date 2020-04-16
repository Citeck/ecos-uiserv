package ru.citeck.ecos.uiserv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.service.dashdoard.dto.OldDashboardDto;
import ru.citeck.ecos.uiserv.service.dashdoard.DashboardEntityService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Roman Makarskiy
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DashboardServiceTest {

    private List<OldDashboardDto> dashboards = new ArrayList<>();

    @Autowired
    private DashboardEntityService dashboardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setup() throws IOException {
        createTestDashboards();
    }

    @Test
    public void createTestActionsSize() {
        assertThat(dashboards.size(), is(3));
    }

    @Test
    public void getById() {
        List<OldDashboardDto> found = dashboards.stream()
            .map(dashboardDTO -> dashboardService.getById(dashboardDTO.getId()).get())
            .collect(Collectors.toList());
        assertThat(found, is(dashboards));
    }

    @Test
    public void getByKey() {
        List<OldDashboardDto> found = dashboards.stream()
            .map(dashboardDTO -> dashboardService.getByKey(dashboardDTO.getType(),
                dashboardDTO.getKey(),
                dashboardDTO.getUser()).get())
            .collect(Collectors.toList());
        assertThat(found, is(dashboards));
    }

    @Test
    public void getByKeys() {
        List<OldDashboardDto> found = dashboards.stream()
            .map(dashboardDTO -> dashboardService.getByKeys(
                dashboardDTO.getType(),
                Arrays.asList("some-key", dashboardDTO.getKey(), "undefined-key"),
                dashboardDTO.getUser()).get())
            .collect(Collectors.toList());
        assertThat(found, is(dashboards));
    }

    //TODO: fix test
    /*@Test
    public void getByRecord() {
        List<DashboardDTO> found = dashboards.stream()
            .map(dashboardDTO -> dashboardService.getByRecord(
                RecordRef.create("dashboard", dashboardDTO.getId())
            ).get())
            .collect(Collectors.toList());
        assertThat(found, is(found));
    }*/

    @Test
    public void save() throws IOException {
        String id = UUID.randomUUID().toString();

        OldDashboardDto dto = new OldDashboardDto();
        dto.setKey("test-key");
        dto.setId(id);
        dto.setConfig(Json.getMapper().read("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", ObjectData.class));

        OldDashboardDto saved = dashboardService.create(dto);
        OldDashboardDto found = dashboardService.getById(id).get();

        assertThat(found, is(saved));
    }

    @Test
    public void saveWithoutId() throws IOException {
        OldDashboardDto dto = new OldDashboardDto();
        dto.setKey("test-key");
        dto.setConfig(Json.getMapper().read("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", ObjectData.class));

        OldDashboardDto saved = dashboardService.create(dto);

        assertThat(dto.getKey(), is(saved.getKey()));
        assertThat(saved.getId(), notNullValue());
        assertThat(dto.getConfig(), is(saved.getConfig()));
    }

    @Test()
    public void saveWithoutKey() throws IOException {
        OldDashboardDto dto = new OldDashboardDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setConfig(Json.getMapper().read("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", ObjectData.class));

        Throwable thrown = Assertions.catchThrowable(() -> dashboardService.create(dto));
        assertEquals("Key is mandatory for creating dashboard", thrown.getMessage());
    }

    @Test
    public void saveWithoutConfig() {
        String id = UUID.randomUUID().toString();

        OldDashboardDto dto = new OldDashboardDto();
        dto.setId(id);
        dto.setKey("some-test-key");

        OldDashboardDto saved = dashboardService.create(dto);
        OldDashboardDto found = dashboardService.getById(id).get();

        assertThat(saved, is(found));
    }

    @Test
    public void mutate() throws IOException {
        String id = UUID.randomUUID().toString();

        OldDashboardDto dto = new OldDashboardDto();
        dto.setId(id);
        dto.setKey("sun-key");
        dto.setConfig(Json.getMapper().read("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", ObjectData.class));

        dashboardService.create(dto);

        OldDashboardDto found = dashboardService.getById(id).get();
        found.setKey("board-test-key");
        found.setConfig(Json.getMapper().read("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"DOWN\"\n" +
            "  }\n" +
            "}", ObjectData.class));

        OldDashboardDto mutated = dashboardService.update(found);
        OldDashboardDto mutatedFound = dashboardService.getById(id).get();

        assertThat(mutated, is(mutatedFound));
    }

    @Test
    public void delete() throws IOException {
        String id = UUID.randomUUID().toString();

        OldDashboardDto dto = new OldDashboardDto();
        dto.setKey("down");
        dto.setId(id);
        dto.setConfig(Json.getMapper().read("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  }\n" +
            "}", ObjectData.class));

        OldDashboardDto saved = dashboardService.create(dto);
        OldDashboardDto found = dashboardService.getById(id).get();

        assertThat(found, is(saved));

        dashboardService.delete(id);

        Optional<OldDashboardDto> mustBeEmpty = dashboardService.getById(id);

        assertThat(mustBeEmpty, is(Optional.empty()));
    }

    private void createTestDashboards() throws IOException {
        OldDashboardDto mainDashboard = new OldDashboardDto();
        mainDashboard.setKey("main-dashboard");
        mainDashboard.setId("main-dashboard-id");
        mainDashboard.setType("user-dashboard");
        mainDashboard.setConfig(Json.getMapper().read("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"TOP\"\n" +
            "  },\n" +
            "  \"layout\": {\n" +
            "    \"type\": \"2-columns-big-small\"\n" +
            "  }\n" +
            "}", ObjectData.class));

        OldDashboardDto contractDashboard = new OldDashboardDto();
        contractDashboard.setKey("contract-dashboard");
        contractDashboard.setId("contract-dashboard-id");
        contractDashboard.setType("case-details");
        contractDashboard.setConfig(Json.getMapper().read("{\n" +
            "    \"_id\": \"5cf91d01f2f927246c3098db\",\n" +
            "    \"index\": 0,\n" +
            "    \"guid\": \"3c30824c-0163-4446-a649-e2af4c3174e3\",\n" +
            "    \"isActive\": false,\n" +
            "    \"balance\": \"$2,697.02\",\n" +
            "    \"age\": 25,\n" +
            "    \"eyeColor\": \"brown\",\n" +
            "    \"name\": {\n" +
            "      \"first\": \"Nina\",\n" +
            "      \"last\": \"Bradley\"\n" +
            "    },\n" +
            "    \"company\": \"ZENSUS\",\n" +
            "    \"email\": \"nina.bradley@zensus.io\"\n" +
            "  }", ObjectData.class));

        OldDashboardDto siteDashboard = new OldDashboardDto();
        siteDashboard.setKey("site-dashboard");
        siteDashboard.setId("site-dashboard-id");
        siteDashboard.setType("site-dashboard");
        siteDashboard.setConfig(Json.getMapper().read("{\n" +
            "  \"menu\": {\n" +
            "    \"type\": \"LEFT\"\n" +
            "  },\n" +
            "  \"layout\": {\n" +
            "    \"type\": \"small\"\n" +
            "  }\n" +
            "}", ObjectData.class));

        dashboards.add(mainDashboard);
        dashboards.add(contractDashboard);
        dashboards.add(siteDashboard);

        dashboards.forEach(dashboardDTO -> dashboardService.create(dashboardDTO));
    }
}
