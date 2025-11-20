package ru.citeck.ecos.uiserv.domain.board.api.records;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.events2.type.RecordEventsService;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue;
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
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

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
    private final WorkspaceService workspaceService;

    private RecordEventsService recordEventsService;

    public static final String ID = "board";
    public static final String LANG_BY_TYPE = "by-type";

    @Autowired
    public BoardRecordsDao(BoardService service, WorkspaceService workspaceService) {
        this.boardService = service;
        this.workspaceService = workspaceService;
    }

    @PostConstruct
    public void init() {
        boardService.onBoardChanged((before, after) -> {
            if (recordEventsService != null) {
                recordEventsService.emitRecChanged(
                    before,
                    after,
                    getId(),
                    def -> new BoardRecord(new BoardWithMeta(def), workspaceService)
                );
            }
        });
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
    public Object getRecordAtts(@NotNull String localBoardId) {
        if (localBoardId.isEmpty()) {
            return new BoardRecord(workspaceService);
        } else {
            IdInWs idInWs = workspaceService.convertToIdInWs(localBoardId);
            BoardWithMeta board = boardService.getBoardById(idInWs);
            if (board == null) {
                log.debug("Board with ID {} was not found", localBoardId);
                return EmptyAttValue.INSTANCE;
            }
            return new BoardRecord(board, workspaceService);
        }
    }

    @Nullable
    @Override
    public RecsQueryRes<BoardRecord> queryRecords(@NotNull RecordsQuery recordsQuery) {

        RecsQueryRes<BoardRecord> result = new RecsQueryRes<>();
        Sort sort = Utils.getSort(recordsQuery);

        if (LANG_BY_TYPE.equals(recordsQuery.getLanguage())) {
            TypeQuery typeQuery = recordsQuery.getQuery(TypeQuery.class);
            if (EntityRef.isEmpty(typeQuery.getTypeRef())) {
                return result;
            }
            List<BoardWithMeta> boards = boardService.getBoardsForExactType(typeQuery.getTypeRef(), sort);
            result.setRecords(boards
                .stream()
                .map(it -> new BoardRecord(it, workspaceService))
                .collect(Collectors.toList()));
            result.setTotalCount(boards.size());
        } else {
            final QueryPage page = recordsQuery.getPage();
            int maxItems = page.getMaxItems();
            int skipCount = page.getSkipCount();
            if (PredicateService.LANGUAGE_PREDICATE.equals(recordsQuery.getLanguage())) {
                Predicate predicate = recordsQuery.getQuery(Predicate.class);
                result.setRecords(
                    boardService.getAll(
                        predicate,
                        recordsQuery.getWorkspaces(),
                        maxItems,
                        skipCount,
                        recordsQuery.getSortBy()
                    ).stream()
                    .map(it -> new BoardRecord(it, workspaceService))
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
        IdInWs idInWs = workspaceService.convertToIdInWs(localId);
        BoardWithMeta boardWithMeta = boardService.getBoardById(idInWs);
        return boardWithMeta != null ?
            new BoardMutRecord(boardWithMeta.getBoardDef(), workspaceService) :
            new BoardMutRecord(localId, workspaceService);
    }

    @NotNull
    @Override
    public String saveMutatedRec(BoardMutRecord boardRecord) {
        checkWritePermissions(boardRecord.getWorkspace());

        String workspace = boardRecord.getWorkspace();
        boardRecord.setTypeRef(prepareExtRef(boardRecord.getTypeRef(), workspace));
        boardRecord.setJournalRef(prepareExtRef(boardRecord.getJournalRef(), workspace));
        boardRecord.setCardFormRef(prepareExtRef(boardRecord.getCardFormRef(), workspace));

        BoardDef boardDef = boardService.save(boardRecord)
            .getBoardDef();
        return workspaceService.addWsPrefixToId(boardDef.getId(), boardDef.getWorkspace());
    }

    private EntityRef prepareExtRef(EntityRef ref, String workspace) {
        return ref.withLocalId(workspaceService.replaceCurrentWsPlaceholderToWsPrefix(ref.getLocalId(), workspace));
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String localId) {
        IdInWs idInWs = workspaceService.convertToIdInWs(localId);
        checkWritePermissions(idInWs.getWorkspace());
        boardService.delete(idInWs);
        return DelStatus.OK;
    }

    @Autowired(required = false)
    public void setRecordEventsService(RecordEventsService recordEventsService) {
        this.recordEventsService = recordEventsService;
    }

    private void checkWritePermissions(String workspace) {
        if (workspaceService.getArtifactsWritePermission(AuthContext.getCurrentUser(), workspace, "board")) {
            return;
        }
        throw new IllegalStateException("Permission denied. You can't create or change boards in workspace '" + workspace + "'");
    }

    @Data
    public static class TypeQuery {
        private EntityRef typeRef;
    }
}
