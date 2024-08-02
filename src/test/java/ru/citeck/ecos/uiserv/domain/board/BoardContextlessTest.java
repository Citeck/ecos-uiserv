package ru.citeck.ecos.uiserv.domain.board;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.context.lib.i18n.I18nContext;
import ru.citeck.ecos.test.commons.EcosWebAppApiMock;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao;
import ru.citeck.ecos.uiserv.domain.board.api.records.ResolvedBoardRecordsDao;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.eapps.BoardArtifactHandler;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.webapp.api.EcosWebAppApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BoardContextlessTest {
    private RecordsServiceFactory recordsServiceFactory;
    private BoardService boardService;
    private BoardArtifactHandler boardArtifactHandler;
    private RecordsService recordsService;

    @BeforeEach
    public void init(){
        EcosWebAppApi webAppContext = new EcosWebAppApiMock(Application.NAME);

        recordsServiceFactory = new RecordsServiceFactory(){
            @Nullable
            @Override
            public EcosWebAppApi getEcosWebAppApi() {
                return webAppContext;
            }
        };
        boardService = new BoardServiceMock(recordsServiceFactory);
        boardArtifactHandler = new BoardArtifactHandler(boardService);
        BoardRecordsDao recordsDao = new BoardRecordsDao(boardService);
        recordsServiceFactory.getRecordsService().register(recordsDao);
        ResolvedBoardRecordsDao resolvedBoardRecordsDao = new ResolvedBoardRecordsDao(recordsDao, null);
        recordsServiceFactory.getRecordsService().register(resolvedBoardRecordsDao);
        //BoardMixin for JournalRecordsDao
        recordsService = recordsServiceFactory.getRecordsService();
    }

    @Test
    public void commonTest(){
        BoardDef boardDef = BoardTestData.getTestBoard();
        testBoard(boardDef);
    }

    private void testBoard(BoardDef boardDef){
        boardArtifactHandler.deployArtifact(boardDef);

        BoardWithMeta result = boardService.getBoardById(boardDef.getId());
        assertEquals(boardDef, result.getBoardDef());

        BoardDef boardFromRecords = recordsService.getAtts(boardDef.getRef(), BoardDef.class);
        //attention: LocalRecordsResolver.getAttsFromSource returns actions=[], columns=[] instead of null value
        assertEquals(boardDef, boardFromRecords);

        String displayName = MLText.getClosestValue(boardDef.getName(), I18nContext.getLocale());
        assertEquals(displayName, recordsService.getAtt(boardDef.getRef(), "?disp").asText());

        recordsService.delete(boardDef.getRef());
        boardFromRecords = recordsService.getAtts(boardDef.getRef(), BoardDef.class);
        assertNull(boardFromRecords.getName());
    }
}
