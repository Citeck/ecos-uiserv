package ru.citeck.ecos.uiserv.domain.board;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.context.lib.i18n.I18nContext;
import ru.citeck.ecos.model.lib.ModelServiceFactory;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
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
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BoardContextlessTest {

    private RecordsServiceFactory recordsServiceFactory;
    private BoardService boardService;
    private BoardArtifactHandler boardArtifactHandler;
    private RecordsService recordsService;
    private WorkspaceService workspaceService;

    @BeforeEach
    public void init(){
        EcosWebAppApi webAppContext = new EcosWebAppApiMock(Application.NAME);

        ModelServiceFactory modelServiceFactory = new ModelServiceFactory();
        workspaceService = modelServiceFactory.getWorkspaceService();

        recordsServiceFactory = new RecordsServiceFactory(){
            @Nullable
            @Override
            public EcosWebAppApi getEcosWebAppApi() {
                return webAppContext;
            }
        };
        boardService = new BoardServiceMock(recordsServiceFactory);
        boardArtifactHandler = new BoardArtifactHandler(boardService);
        BoardRecordsDao recordsDao = new BoardRecordsDao(boardService, workspaceService);
        recordsServiceFactory.getRecordsService().register(recordsDao);
        ResolvedBoardRecordsDao resolvedBoardRecordsDao = new ResolvedBoardRecordsDao(recordsDao, null, workspaceService);
        recordsServiceFactory.getRecordsService().register(resolvedBoardRecordsDao);
        //BoardMixin for JournalRecordsDao
        recordsService = recordsServiceFactory.getRecordsService();

        modelServiceFactory.setRecordsServices(recordsServiceFactory);
    }

    @Test
    public void commonTest(){
        BoardDef boardDef = BoardTestData.getTestBoard();
        testBoard(boardDef);
    }

    private void testBoard(BoardDef boardDef){
        boardArtifactHandler.deployArtifact(boardDef);

        BoardWithMeta result = boardService.getBoardById(IdInWs.create(boardDef.getId()));
        assertEquals(boardDef, result.getBoardDef());


        BoardDef boardFromRecords = recordsService.getAtts(getRef(boardDef), BoardDef.class);
        //attention: LocalRecordsResolver.getAttsFromSource returns actions=[], columns=[] instead of null value
        assertEquals(boardDef, boardFromRecords);

        String displayName = MLText.getClosestValue(boardDef.getName(), I18nContext.getLocale());
        assertEquals(displayName, recordsService.getAtt(getRef(boardDef), "?disp").asText());

        recordsService.delete(getRef(boardDef));
        boardFromRecords = recordsService.getAtts(getRef(boardDef), BoardDef.class);
        assertNull(boardFromRecords.getName());
    }

    private EntityRef getRef(BoardDef boardDef) {
        String localId = workspaceService.addWsPrefixToId(boardDef.getId(), boardDef.getWorkspace());
        return EntityRef.create(AppName.UISERV, BoardRecordsDao.ID, localId);
    }
}
