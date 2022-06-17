package ru.citeck.ecos.uiserv.domain.board.api.records;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.TestEntityRecordUtil;
import ru.citeck.ecos.uiserv.TestUtil;
import ru.citeck.ecos.uiserv.domain.board.BoardTestData;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(EcosSpringExtension.class)
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
        String jsonString = DataValue.createObj()
            .set(QUERY,
                DataValue.createObj().set("sourceId", BoardRecordsDao.ID)
                    .set(LANGUAGE, BoardRecordsDao.LANG_BY_TYPE)
                    .set(QUERY, DataValue.createObj().set("typeRef", BoardTestData.testTypeRef.toString()))).toString();

        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(TestEntityRecordUtil.URL_RECORDS_QUERY)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isEmpty());
    }

    @Test
    public void createRecord() throws Exception {
        String jsonString = getJsonToSend(DataValue.createObj()
            .set(RECORDS, DataValue.createArr().add(
                DataValue.createObj().set(BoardTestData.PROP_ID, BoardTestData.getEmptyId())
                    .set(ATTRIBUTES, DataValue.createObj()
                        .set(BoardTestData.PROP_NAME, "TestBoard - Create")
                        .set(BoardTestData.PROP_READ_ONLY, false))
            )).toString());

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

        String jsonString = getJsonToSend(DataValue.createObj()
            .set(RECORDS, DataValue.createArr().add(
                DataValue.createObj().set(BoardTestData.PROP_ID, BoardTestData.getEmptyId() + BoardTestData.BOARD_ID)
                    .set(ATTRIBUTES, DataValue.createObj()
                        .set(BoardTestData.PROP_NAME, newName)
                        .set(BoardTestData.PROP_COLUMNS, DataValue.createArr()
                            .add(DataValue.createObj().set("id", "col-id1")
                                .set("name", "First Column Name"))
                            .add(DataValue.createObj().set("id", "col-id2")
                                .set("name", "Second Column Name"))
                        ))
            )).toString());

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
        String jsonString = getJsonToSend(DataValue.createObj()
            .set(RECORDS, DataValue.createArr().add(BoardTestData.getEmptyId() + BoardTestData.BOARD_ID
            )).toString());

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
        BoardDef boardDef = BoardTestData.getTestBoard();
        service.save(boardDef);
    }

    private void deleteTestBoardDef() {
        service.delete(BoardTestData.BOARD_ID);
    }

    private void deleteAll() {
        repository.deleteAll();
        repository.flush();
    }

    private static String getTestBoardJson() throws Exception {
        if (testBoardJson != null) {
            return testBoardJson;
        }
        testBoardJson = getJsonToSend(DataValue.createObj()
            .set(RECORDS, DataValue.createArr().add(BoardTestData.getEmptyId() + BoardTestData.BOARD_ID))
            .set(ATTRIBUTES, DataValue.createArr()
                .add(BoardTestData.PROP_ID + STR)
                .add(BoardTestData.PROP_READ_ONLY)
                .add(BoardTestData.PROP_NAME + STR)
                .add(BoardTestData.PROP_COLUMNS + "[]" + STR))
            .toString());
        return testBoardJson;
    }

    private static String queryTestBoardJson() throws Exception {
        if (queryBoardJson != null) {
            return queryBoardJson;
        }
        queryBoardJson = getJsonToSend(DataValue.createObj()
            .set(QUERY,
                DataValue.createObj().set("sourceId", BoardTestData.UISERV_APP_ID + "/" + BoardRecordsDao.ID)
                    .set(LANGUAGE, PredicateService.LANGUAGE_PREDICATE)
                    .set("page", DataValue.createObj().set("maxItems", 10))
                    .set(QUERY, DataValue.createObj()
                        .set("t", "eq")
                        .set("att", BoardTestData.PROP_ID)
                        .set("val", BoardTestData.BOARD_ID)
                    ))
            .set(ATTRIBUTES, DataValue.createObj()
                .set(BoardTestData.PROP_NAME, BoardTestData.PROP_NAME)
                .set(BoardTestData.PROP_ID, BoardTestData.PROP_ID)).toString());
        return queryBoardJson;
    }

    private static String getJsonToSend(String json) {
        return json.replace("\\/", "/");
    }
}
