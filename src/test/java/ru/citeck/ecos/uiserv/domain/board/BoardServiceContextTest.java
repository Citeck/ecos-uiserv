package ru.citeck.ecos.uiserv.domain.board;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class BoardServiceContextTest {

    @Autowired
    private BoardService service;

    @Test
    public void baseTest(){
        String id = "test-board";
        BoardDef boardDef = new BoardDef();
        boardDef.setId(id);
        boardDef.setName(new MLText("Test board"));
        boardDef.setTypeRef(RecordRef.valueOf("emodel/type@user-board"));
        service.save(boardDef);
        Matcher<Optional> boardDefOprional = is(Optional.of(boardDef));
        assertThat(service.getBoardById(id), boardDefOprional);

        service.delete(id);
        assertThat(service.getBoardById(id), is(Optional.empty()));

        boardDef.setColumns(Arrays.asList(new BoardColumnDef("first", new MLText("1st Column")), new BoardColumnDef("second", new MLText("2d Column"))));
        service.save(boardDef);
        assertThat(service.getBoardById(id), boardDefOprional);

        boardDef.setReadOnly(true);
        service.save(boardDef);
        assertThat(service.getBoardById(id), boardDefOprional);

        List<BoardDef> list = service.getBordsForExactType(boardDef.getTypeRef());
        assertTrue(list!=null);
        assertNotNull(list.get(0));
        assertThat(list.get(0), is(boardDef));

    }
}
