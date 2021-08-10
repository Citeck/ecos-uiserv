package ru.citeck.ecos.uiserv.domain.board;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records3.RecordsProperties;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao;
import ru.citeck.ecos.uiserv.domain.board.api.records.ResolvedBoardRecordsDao;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.eapps.BoardArtifactHandler;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BoardContextlessTest {
    RecordsServiceFactory recordsServiceFactory;
    BoardService boardService;
    BoardArtifactHandler boardArtifactHandler;
    RecordsService recordsService;

    @BeforeEach
    private void init(){
        recordsServiceFactory = new RecordsServiceFactory(){
            @NotNull
            @Override
            protected RecordsProperties createProperties() {
                RecordsProperties properties = super.createProperties();
                properties.setAppInstanceId("102030");
                properties.setAppName(BoardEntity.APP_NAME);
                return properties;
            }
        };
        boardService = new BoardServiceMock(recordsServiceFactory);
        boardArtifactHandler = new BoardArtifactHandler(boardService);
        BoardRecordsDao recordsDao = new BoardRecordsDao(boardService);
        recordsServiceFactory.getRecordsServiceV1().register(recordsDao);
        ResolvedBoardRecordsDao resolvedBoardRecordsDao = new ResolvedBoardRecordsDao(recordsDao, null);
        recordsServiceFactory.getRecordsServiceV1().register(resolvedBoardRecordsDao);
        //BoardMixin for JournalRecordsDao
        recordsService = recordsServiceFactory.getRecordsServiceV1();
    }

    @Test
    public void commonTest(){
        BoardDef boardDef = BoardTestData.getTestBoard();
        testBoard(boardDef);
    }

    private void testBoard(BoardDef boardDef){
        boardArtifactHandler.deployArtifact(boardDef);

        Optional<BoardWithMeta> result = boardService.getBoardById(boardDef.getId());
        assertEquals(boardDef, result.get().getBoardDef());

        BoardDef boardFromRecords = recordsService.getAtts(boardDef.getRef(), BoardDef.class);
        //attention: LocalRecordsResolver.getAttsFromSource returns actions=[], columns=[] instead of null value
        assertEquals(boardDef, boardFromRecords);

        String displayName = MLText.getClosestValue(boardDef.getName(), RequestContext.getLocale());
        assertEquals(displayName, recordsService.getAtt(boardDef.getRef(), "?disp").asText());

        recordsService.delete(boardDef.getRef());
        boardFromRecords = recordsService.getAtts(boardDef.getRef(), BoardDef.class);
        assertNull(boardFromRecords.getName());
    }
}
