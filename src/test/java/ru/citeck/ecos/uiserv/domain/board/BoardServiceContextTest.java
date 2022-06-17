package ru.citeck.ecos.uiserv.domain.board;

import org.hamcrest.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardRepository;
import ru.citeck.ecos.uiserv.domain.board.service.BoardMapper;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
@DirtiesContext
public class BoardServiceContextTest {

    @Autowired
    private BoardService service;
    @Autowired
    private BoardRepository repository;

    @BeforeEach
    public void createTestBoard(){
        repository.save(BoardMapper.dtoToEntity(repository, BoardTestData.getTestBoard()));
    }

    @Test
    public void createTest() {
        BoardDef boardDef = BoardTestData.getNewBoard();
        BoardWithMeta boardWithMeta = service.save(boardDef);
        assertNotNull(boardWithMeta.getModifier());

        BoardDef createdBoardDef = boardWithMeta.getBoardDef();
        boardDef.setId(createdBoardDef.getId());

        Matcher<BoardDef> boardDefMatcher = is(boardDef);
        assertThat(createdBoardDef, boardDefMatcher);
    }

    @Test
    public void deleteTest() {
        service.delete(BoardTestData.BOARD_ID);
        assertNull(service.getBoardById(BoardTestData.BOARD_ID));
    }

    @Test
    public void modifyTest() {
        BoardDef boardDef = BoardTestData.getTestBoard();
        service.save(boardDef);
        Matcher<BoardDef> boardDefMatcher = is(boardDef);
        assertThat(service.getBoardById(BoardTestData.BOARD_ID).getBoardDef(), boardDefMatcher);

        boardDef.setColumns(Arrays.asList(new BoardColumnDef("first", new MLText("1st Column")), new BoardColumnDef("second", new MLText("2d Column"))));
        service.save(boardDef);
        assertThat(service.getBoardById(BoardTestData.BOARD_ID).getBoardDef(), boardDefMatcher);

        boardDef.setReadOnly(true);
        service.save(boardDef);
        assertThat(service.getBoardById(BoardTestData.BOARD_ID).getBoardDef(), boardDefMatcher);
    }

    @Test
    public void selectForExactTypeTest() {
        BoardDef boardDef = BoardTestData.getTestBoard();

        List<BoardWithMeta> list = service.getBoardsForExactType(boardDef.getTypeRef(), null);
        assertNotNull(list);
        assertNotNull(list.get(0));
        assertThat(list.get(0).getBoardDef(), is(boardDef));
    }

    @Test
    public void deleteNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.delete(null));
    }
}
