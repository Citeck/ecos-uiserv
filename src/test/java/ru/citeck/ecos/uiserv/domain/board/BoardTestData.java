package ru.citeck.ecos.uiserv.domain.board;

import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;

public class BoardTestData {

    public static final String UISERV_APP_ID = "uiserv";
    public static String ID = "test-board";
    public static String PROP_ID = "id";
    public static String PROP_NAME = "name";
    public static String PROP_READ_ONLY = "readOnly";
    public static String PROP_TYPE = "typeRef";
    public static String PROP_COLUMNS = "columns";
    public static final BoardDef testBoard = new BoardDef(ID);
    public static final RecordRef testTypeRef = RecordRef.create("emodel", "type", "testType");

    static {
        testBoard.setName(new MLText("Test board"));
        testBoard.setTypeRef(RecordRef.valueOf("emodel/type@user-board"));
    }

    public static BoardDef getTestBoard() {
        return new BoardDef(testBoard);
    }

    public static String getEmptyId() {
        return UISERV_APP_ID +"/"+ BoardRecordsDao.ID+"@";
    }

    public static BoardDef getNewBoard() {
        BoardDef boardDef = new BoardDef(testBoard);
        boardDef.setId(null);
        return boardDef;
    }
}
