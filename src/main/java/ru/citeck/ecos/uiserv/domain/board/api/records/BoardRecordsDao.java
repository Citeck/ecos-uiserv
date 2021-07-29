package ru.citeck.ecos.uiserv.domain.board.api.records;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

public class BoardRecordsDao implements RecordAttsDao {
    private BoardService boardService;
    public static final String ID = "board";

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String localBoardId) {
        if (localBoardId.isEmpty()) {
            return new BoardWithMeta();
        } else {
            //boardService.getBoardById()
        }
        return null;
    }
}
