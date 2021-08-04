package ru.citeck.ecos.uiserv.domain.board.api.records;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.TestEntityRecordUtil;
import ru.citeck.ecos.uiserv.TestUtil;
import ru.citeck.ecos.uiserv.domain.board.BoardTestData;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BoardRecordsDaoTest {

    static String QUERY = "query";
    static String LANGUAGE = "language";
    static String RECORDS = "records";
    static String ATTRIBUTES = "attributes";
    static String NAME_ATTRIBUTE = "name";
    static String READ_ONLY_ATTRIBUTE = "readOnly";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordsService recordsService;

    @Test
    public void queryByTypeRef_WithEmptyResult() throws Exception {
        String jsonString = new JSONObject()
            .put(QUERY,
                new JSONObject().put("sourceId", BoardRecordsDao.ID)
                    .put(LANGUAGE, BoardRecordsDao.BY_TYPE)
                    .put(QUERY, new JSONObject().put("typeRef", BoardTestData.testTypeRef.toString()))).toString(2);

        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_QUERY)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.records").isEmpty());
    }

    @Test
    public void createRecord() throws Exception {
        //{"records":[{"id":"uiserv/board@", "attributes":{"name?str":"test111", "system?bool":true}}], "version":1}
        String jsonString = new JSONObject()
            .put(RECORDS, new JSONArray().put(
                new JSONObject().put(BoardTestData.PROP_ID, BoardTestData.getEmptyId())
                    .put(ATTRIBUTES, new JSONObject()
                        .put(BoardTestData.PROP_NAME, "TestBoard - Create")
                        .put(BoardTestData.PROP_READ_ONLY, false))
            )).toString(2);
        jsonString = jsonString.replace("\\/", "/");
        System.out.println(jsonString);
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_MUTATE)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.records").isNotEmpty())
            .andDo(print());
    }

    public void updateRecord(){

    }

    public void deleteRecord(){

    }

    public void selectRecords(){

    }

}
