package ru.citeck.ecos.uiserv.domain.board;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@DirtiesContext
public class BoardServiceContextTest {

    @Autowired
    private BoardService service;

    @Test
    public void createTest() {
        BoardDef boardDef = BoardTestData.getNewBoard();
        BoardDef createdBoardDef = service.save(boardDef).getBoardDef();
        boardDef.setId(createdBoardDef.getId());

        Matcher<BoardDef> boardDefMatcher = is(boardDef);
        assertThat(createdBoardDef, boardDefMatcher);
    }

    @Test
    public void baseTest() {
        BoardDef boardDef = BoardTestData.getTestBoard();
        service.save(boardDef);
        Matcher<BoardDef> boardDefMatcher = is(boardDef);
        assertThat(service.getBoardById(BoardTestData.ID).get().getBoardDef(), boardDefMatcher);

        service.delete(BoardTestData.ID);
        assertThat(service.getBoardById(BoardTestData.ID), is(Optional.empty()));

        boardDef.setColumns(Arrays.asList(new BoardColumnDef("first", new MLText("1st Column")), new BoardColumnDef("second", new MLText("2d Column"))));
        service.save(boardDef);
        assertThat(service.getBoardById(BoardTestData.ID).get().getBoardDef(), boardDefMatcher);

        boardDef.setReadOnly(true);
        service.save(boardDef);
        assertThat(service.getBoardById(BoardTestData.ID).get().getBoardDef(), boardDefMatcher);

        List<BoardWithMeta> list = service.getBoardsForExactType(boardDef.getTypeRef());
        assertTrue(list != null);
        assertNotNull(list.get(0));
        assertThat(list.get(0).getBoardDef(), is(boardDef));
    }

    @Test
    public void deleteNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.delete(null));
    }
}
