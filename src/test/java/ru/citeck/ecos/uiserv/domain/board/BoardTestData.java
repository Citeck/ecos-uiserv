package ru.citeck.ecos.uiserv.domain.board;

import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;

import java.util.Arrays;
import java.util.Locale;

public class BoardTestData {

    public static final String UISERV_APP_ID = Application.NAME;
    public static String BOARD_ID = "test-board";
    public static String PROP_ID = "id";
    public static String PROP_NAME = "name";
    public static String PROP_READ_ONLY = "readOnly";
    public static String PROP_COLUMNS = "columns";
    public static final BoardDef testBoard = new BoardDef(BOARD_ID);
    public static final RecordRef testTypeRef = RecordRef.create("emodel", "type", "testType");

    static {
        testBoard.setName(new MLText("Test board").withValue(Locale.GERMAN, "German name"));
        testBoard.setTypeRef(RecordRef.valueOf("emodel/type@user-board"));
        testBoard.setActions(Arrays.asList(RecordRef.valueOf("uiserv/action@test0")));
        testBoard.setColumns(Arrays.asList(
            BoardColumnDef.create().withId("col1").withName(new MLText("First Column")).build(),
            BoardColumnDef.create().withId("col2").withName(new MLText("Second Column")).build()));
    }

    public static BoardDef getTestBoard() {
        return new BoardDef(testBoard);
    }

    public static String getEmptyId() {
        return UISERV_APP_ID + "/" + BoardRecordsDao.ID + "@";
    }

    public static BoardDef getNewBoard() {
        BoardDef boardDef = new BoardDef(testBoard);
        boardDef.setId(null);
        return boardDef;
    }
}
