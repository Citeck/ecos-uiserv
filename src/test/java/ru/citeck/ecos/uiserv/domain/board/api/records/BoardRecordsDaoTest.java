package ru.citeck.ecos.uiserv.domain.board.api.records;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.TestEntityRecordUtil;
import ru.citeck.ecos.uiserv.TestUtil;
import ru.citeck.ecos.uiserv.domain.board.BoardTestData;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;
import ru.citeck.ecos.uiserv.domain.board.service.BoardMapper;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

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
    static String STR = "?str";
    private static String testBoardJson;
    private static String queryBoardJson;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardService service;

    @Autowired
    private BoardRepository repository;

    @Test
    public void queryByTypeRef_WithEmptyResult() throws Exception {
        String jsonString = new JSONObject()
            .put(QUERY,
                new JSONObject().put("sourceId", BoardRecordsDao.ID)
                    .put(LANGUAGE, BoardRecordsDao.LANG_BY_TYPE)
                    .put(QUERY, new JSONObject().put("typeRef", BoardTestData.testTypeRef.toString()))).toString(2);

        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_QUERY)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isEmpty());
    }

    @Test
    public void createRecord() throws Exception {
        String jsonString = getJsonToSend(new JSONObject()
            .put(RECORDS, new JSONArray().put(
                new JSONObject().put(BoardTestData.PROP_ID, BoardTestData.getEmptyId())
                    .put(ATTRIBUTES, new JSONObject()
                        .put(BoardTestData.PROP_NAME, "TestBoard - Create")
                        .put(BoardTestData.PROP_READ_ONLY, false))
            )).toString(2));

        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_MUTATE)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isNotEmpty());
    }

    @Test
    public void updateRecord() throws Exception {
        createTestBoardDef();
        String newName = "Updated TestBoard";

        String jsonString = getJsonToSend(new JSONObject()
            .put(RECORDS, new JSONArray().put(
                new JSONObject().put(BoardTestData.PROP_ID, BoardTestData.getEmptyId() + BoardTestData.BOARD_ID)
                    .put(ATTRIBUTES, new JSONObject()
                        .put(BoardTestData.PROP_NAME, newName)
                        .put(BoardTestData.PROP_COLUMNS, new JSONArray()
                            .put(new JSONObject().put("id", "col-id1")
                                .put("name", "First Column Name"))
                            .put(new JSONObject().put("id", "col-id2")
                                .put("name", "Second Column Name"))
                        ))
            )).toString(2));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_MUTATE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isNotEmpty());

        //get board, check name value
        resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(getTestBoardJson()));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isNotEmpty())
            .andExpect(jsonPath("$.." + BoardTestData.PROP_NAME + STR).value(newName));
    }

    @Test
    public void deleteRecord() throws Exception {
        createTestBoardDef();
        String jsonString = getJsonToSend(new JSONObject()
            .put(RECORDS, new JSONArray().put(BoardTestData.getEmptyId() + BoardTestData.BOARD_ID
            )).toString(2));

        mockMvc.perform(
                MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_DELETE)
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(jsonString))
            .andExpect(status().isOk());

        mockMvc.perform(
                MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(getTestBoardJson()))
            .andDo(print());
        //.andExpect(jsonPath("$.." + BoardTestData.PROP_NAME + STR).value(IsNull.nullValue()));
    }

    @Test
    public void queryByPredicates_withOneResult() throws Exception {
        deleteAll();
        createTestBoardDef();
        final ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(queryTestBoardJson()));
        resultActions
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isNotEmpty())
            .andExpect(jsonPath("$." + RECORDS + "[0]." + BoardTestData.PROP_ID)
                .value(BoardTestData.getEmptyId() + BoardTestData.BOARD_ID))
            .andExpect(jsonPath("$.." + BoardTestData.PROP_NAME)
                .value(BoardTestData.testBoard.getName().get()));
    }

    @Test
    public void queryByPredicates_withEmptyResult() throws Exception {
        deleteAll();
        mockMvc.perform(
                MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_QUERY)
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(queryTestBoardJson()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isEmpty());
    }

    private void createTestBoardDef() {
        repository.save(BoardMapper.dtoToEntity(repository, BoardTestData.getTestBoard()));
    }

    private void deleteTestBoardDef() {
        repository.delete(BoardMapper.dtoToEntity(repository, BoardTestData.getTestBoard()));
    }

    private void deleteAll() {
        repository.deleteAll();
        repository.flush();
    }

    private static String getTestBoardJson() throws Exception {
        if (testBoardJson != null) {
            return testBoardJson;
        }
        testBoardJson = getJsonToSend(new JSONObject()
            .put(RECORDS, new JSONArray().put(BoardTestData.getEmptyId() + BoardTestData.BOARD_ID))
            .put(ATTRIBUTES, new JSONArray()
                .put(BoardTestData.PROP_ID + STR)
                .put(BoardTestData.PROP_READ_ONLY)
                .put(BoardTestData.PROP_NAME + STR)
                .put(BoardTestData.PROP_COLUMNS + "[]" + STR))
            .toString(2));
        return testBoardJson;
    }

    private static String queryTestBoardJson() throws Exception {
        if (queryBoardJson != null) {
            return queryBoardJson;
        }
        queryBoardJson = getJsonToSend(new JSONObject()
            .put(QUERY,
                new JSONObject().put("sourceId", BoardTestData.UISERV_APP_ID + "/" + BoardRecordsDao.ID)
                    .put(LANGUAGE, PredicateService.LANGUAGE_PREDICATE)
                    .put("page", new JSONObject().put("maxItems", 10))
                    .put(QUERY, new JSONObject()
                        .put("t", "eq")
                        .put("att", BoardTestData.PROP_ID)
                        .put("val", BoardTestData.BOARD_ID)
                    ))
            .put(ATTRIBUTES, new JSONObject()
                .put(BoardTestData.PROP_NAME, BoardTestData.PROP_NAME)
                .put(BoardTestData.PROP_ID, BoardTestData.PROP_ID)).toString(2));
        return queryBoardJson;
    }

    private static String getJsonToSend(String json) {
        return json.replace("\\/", "/");
    }
}
