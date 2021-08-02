package ru.citeck.ecos.uiserv.domain.board.api.records;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

import java.util.List;

@Component
public class BoardRecordsDao implements RecordAttsDao, RecordsQueryDao, RecordMutateDtoDao<BoardDef>, RecordDeleteDao {

    private static final Logger log = LoggerFactory.getLogger(BoardRecordsDao.class);
    @Autowired
    private BoardService boardService;
    public static final String ID = "board";
    public static final String BY_TYPE = "by-type";

    /**
     * Returns this data source identifier
     */
    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    /**
     * Returns a BoardWithMeta
     *
     * @param localBoardId not null board local ID
     * @throws IllegalArgumentException if board with ID localBoardId was not found
     */
    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String localBoardId) {
        if (localBoardId.isEmpty()) {
            return new BoardWithMeta();
        } else {
            return boardService.getBoardById(localBoardId)
                .orElseThrow(() -> new IllegalArgumentException("Board with ID \'" + localBoardId + "\' was not found"));
        }
    }

    @Nullable
    @Override
    public Object queryRecords(@NotNull RecordsQuery recordsQuery) {

        RecsQueryRes<BoardWithMeta> result = new RecsQueryRes<>();
        if (recordsQuery.getLanguage() == BY_TYPE) {
            TypeQuery typeQuery = recordsQuery.getQuery(TypeQuery.class);
            if (RecordRef.isEmpty(typeQuery.getTypeRef())) {
                return result;
            }
            List<BoardWithMeta> boards = boardService.getBoardsForExactType(typeQuery.getTypeRef());
            result.setRecords(boards);
            result.setTotalCount(boards.size());
        } else{
            final QueryPage page = recordsQuery.getPage();
            int maxItemsCount = page.getMaxItems() <= 0 ? 10000 : page.getMaxItems();
            int skipCount = page.getSkipCount();
            if (recordsQuery.getLanguage() == PredicateService.LANGUAGE_PREDICATE) {
                Predicate predicate = recordsQuery.getQuery(Predicate.class);
                result.setRecords(boardService.getAll(maxItemsCount, skipCount, predicate));
                result.setTotalCount(boardService.getCount(predicate));
            } else {
                log.warn("Unsupported query language '{}'", recordsQuery.getLanguage());
                result.setRecords(boardService.getAll(maxItemsCount, skipCount, null));
                result.setTotalCount(boardService.getCount());
            }
        }
        //AbstractRecordsDao: predicateService
        return result;
    }

    @Override
    public BoardDef getRecToMutate(@NotNull String localId) {
        return boardService.getBoardById(localId).map(boardWithMeta -> new BoardDef(boardWithMeta.getBoardDef())).orElse(new BoardDef(localId));
    }

    @NotNull
    @Override
    public String saveMutatedRec(BoardDef boardDef) {
        return boardService.save(boardDef).getBoardDef().getId();
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String localId) {
        boardService.delete(localId);
        return DelStatus.OK;
    }

    /*It would be nice to make a parent interface contained typeRef (getter/setter), as
     * typeRef is used in several cases (Journals, Forms, Action, here) */
    @Data
    public static class TypeQuery {
        private RecordRef typeRef;
    }
}
