package ru.citeck.ecos.uiserv.domain.board.api.records;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.BoardTestData;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
public class BoardRecordsDaoTest {

    static String STR = "?str";

    @Autowired
    private BoardService service;

    @Autowired
    private BoardRepository repository;

    @Autowired
    private RecordsService recordsService;

    @AfterEach
    public void afterEach() {
        repository.deleteAll();
        repository.flush();
    }

    @Test
    public void queryByTypeRef_WithEmptyResult() {

        RecsQueryRes<EntityRef> res = recordsService.query(RecordsQuery.create()
            .withSourceId(BoardRecordsDao.ID)
            .withLanguage(BoardRecordsDao.LANG_BY_TYPE)
            .withQuery(DataValue.createObj().set("typeRef", BoardTestData.testTypeRef.toString()))
            .build());

        assertThat(res.getRecords()).isEmpty();
    }

    @Test
    public void createRecord() {

        recordsService.mutate(BoardTestData.getEmptyId(), DataValue.createObj()
            .set(BoardTestData.PROP_NAME, "TestBoard - Create")
            .set(BoardTestData.PROP_READ_ONLY, false));

        RecsQueryRes<EntityRef> res = recordsService.query(RecordsQuery.create()
            .withSourceId(BoardRecordsDao.ID)
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .withQuery(Predicates.alwaysTrue())
            .build());

        assertThat(res.getRecords()).hasSize(1);
    }

    @Test
    public void updateRecord() {

        createTestBoardDef();
        String newName = "Updated TestBoard";

        recordsService.mutate(BoardTestData.getEmptyId() + BoardTestData.BOARD_ID,
            DataValue.createObj()
                .set(BoardTestData.PROP_NAME, newName)
                .set(BoardTestData.PROP_COLUMNS, DataValue.createArr()
                    .add(DataValue.createObj().set("id", "col-id1")
                        .set("name", "First Column Name"))
                    .add(DataValue.createObj().set("id", "col-id2")
                        .set("name", "Second Column Name"))
                )
        );

        RecordAtts testBoard = getTestBoardJson();

        assertThat(testBoard.getAtt(BoardTestData.PROP_NAME + STR).asText()).isEqualTo(newName);
    }

    @Test
    public void deleteRecord() {
        createTestBoardDef();

        val ref = EntityRef.valueOf(BoardTestData.getEmptyId() + BoardTestData.BOARD_ID);
        assertThat(recordsService.getAtt(ref, "_notExists?bool").asBoolean()).isFalse();
        recordsService.delete(ref);
        assertThat(recordsService.getAtt(ref, "_notExists?bool").asBoolean()).isTrue();
    }

    @Test
    public void queryByPredicates_withOneResult() {
        createTestBoardDef();

        val testRecData = queryTestBoard();
        assertThat(testRecData).isNotEmpty();
        assertThat(testRecData.getFirst().get(BoardTestData.PROP_ID).asText()).isEqualTo(BoardTestData.BOARD_ID);
        assertThat(testRecData.getFirst()
            .get(BoardTestData.PROP_NAME).asText())
            .isEqualTo(BoardTestData.testBoard.getName().get());
    }

    @Test
    public void queryByPredicates_withEmptyResult() {
        assertThat(queryTestBoard()).isEmpty();
    }

    private void createTestBoardDef() {
        BoardDef boardDef = BoardTestData.getTestBoard();
        service.save(boardDef);
    }

    private RecordAtts getTestBoardJson() {

        return recordsService.getAtts(
            BoardTestData.getEmptyId() + BoardTestData.BOARD_ID,
            List.of(
                BoardTestData.PROP_ID + STR,
                BoardTestData.PROP_READ_ONLY,
                BoardTestData.PROP_NAME + STR,
                BoardTestData.PROP_COLUMNS + "[]" + STR
            )
        );
    }

    private List<RecordAtts> queryTestBoard() {
        return recordsService.query(
            RecordsQuery.create()
                .withSourceId(BoardRecordsDao.ID)
                .withQuery(Predicates.eq(BoardTestData.PROP_ID, BoardTestData.BOARD_ID))
                .build(),
            Map.of(
                BoardTestData.PROP_NAME, BoardTestData.PROP_NAME,
                BoardTestData.PROP_ID, BoardTestData.PROP_ID
            )
        ).getRecords();
    }
}
