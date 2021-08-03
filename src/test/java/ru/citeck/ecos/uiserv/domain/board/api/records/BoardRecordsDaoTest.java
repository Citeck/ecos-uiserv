package ru.citeck.ecos.uiserv.domain.board.api.records;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.BoardTestData;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)

public class BoardRecordsDaoTest {

    static String QUERY_URL = "/api/records/query";
    static String CONTENT_TYPE = "Content-type";
    static String JSON_CONTENT_TYPE = "application/json";
    static String QUERY = "query";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordsService recordsService;

    //@Test
    public void queryByTypeRef_WithEmptyResult() throws Exception {

        String jsonString = new JSONObject()
            .put(QUERY,
                new JSONObject().put("sourceId", BoardRecordsDao.ID)
                    .put(QUERY, new JSONObject().put("typeRef", BoardTestData.testTypeRef.toString()))).toString(2);
        System.out.println(mockMvc);

        mockMvc.perform(MockMvcRequestBuilders.post(QUERY_URL)
                .contentType(JSON_CONTENT_TYPE)
                .header(CONTENT_TYPE, JSON_CONTENT_TYPE)
                .content(jsonString))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records", is(Collections.emptyList())));
    }
}
