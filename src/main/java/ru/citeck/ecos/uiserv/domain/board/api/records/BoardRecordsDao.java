package ru.citeck.ecos.uiserv.domain.board.api.records;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.uiserv.app.common.api.records.Utils;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BoardRecordsDao extends AbstractRecordsDao implements RecordAttsDao,
    RecordsQueryDao,
    RecordMutateDtoDao<BoardMutRecord>,
    RecordDeleteDao {

    private final BoardService boardService;
    public static final String ID = "board";
    public static final String LANG_BY_TYPE = "by-type";

    @Autowired
    public BoardRecordsDao(BoardService service) {
        this.boardService = service;
    }

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
     */
    @Nullable
    @Override
    public BoardRecord getRecordAtts(@NotNull String localBoardId) {
        if (localBoardId.isEmpty()) {
            return new BoardRecord();
        } else {
            BoardWithMeta board = boardService.getBoardById(localBoardId);
            if (board == null) {
                log.warn("Board with ID {} was not found", localBoardId);
                return new BoardRecord(localBoardId);
            }
            return new BoardRecord(board);
        }
    }

    @Nullable
    @Override
    public RecsQueryRes<BoardRecord> queryRecords(@NotNull RecordsQuery recordsQuery) {

        RecsQueryRes<BoardRecord> result = new RecsQueryRes<>();
        Sort sort = Utils.getSort(recordsQuery);

        if (LANG_BY_TYPE.equals(recordsQuery.getLanguage())) {
            TypeQuery typeQuery = recordsQuery.getQuery(TypeQuery.class);
            if (RecordRef.isEmpty(typeQuery.getTypeRef())) {
                return result;
            }
            List<BoardWithMeta> boards = boardService.getBoardsForExactType(typeQuery.getTypeRef(), sort);
            result.setRecords(boards
                .stream()
                .map(BoardRecord::new)
                .collect(Collectors.toList()));
            result.setTotalCount(boards.size());
        } else {
            final QueryPage page = recordsQuery.getPage();
            int maxItemsCount = page.getMaxItems() <= 0 ? 10000 : page.getMaxItems();
            int skipCount = page.getSkipCount();
            if (PredicateService.LANGUAGE_PREDICATE.equals(recordsQuery.getLanguage())) {
                Predicate predicate = recordsQuery.getQuery(Predicate.class);
                result.setRecords(boardService.getAll(maxItemsCount, skipCount, predicate, sort)
                    .stream()
                    .map(BoardRecord::new)
                    .collect(Collectors.toList()));
                result.setTotalCount(boardService.getCount(predicate));
            } else {
                log.warn("Unsupported query language '{}'", recordsQuery.getLanguage());
                result.setRecords(Collections.emptyList());
                result.setTotalCount(0);
            }
        }
        return result;
    }

    @Override
    public BoardMutRecord getRecToMutate(@NotNull String localId) {
        BoardWithMeta boardWithMeta = boardService.getBoardById(localId);
        return boardWithMeta != null ?
            new BoardMutRecord(boardWithMeta.getBoardDef()) :
            new BoardMutRecord(localId);
    }

    @NotNull
    @Override
    public String saveMutatedRec(BoardMutRecord boardRecord) {
        return boardService.save(boardRecord)
            .getBoardDef()
            .getId();
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String localId) {
        boardService.delete(localId);
        return DelStatus.OK;
    }

    @Data
    public static class TypeQuery {
        private RecordRef typeRef;
    }
}
