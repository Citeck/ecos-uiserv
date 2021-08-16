package ru.citeck.ecos.uiserv.domain.board.api.records;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService;

@Component
@RequiredArgsConstructor
public class ResolvedBoardRecordsDao extends AbstractRecordsDao implements RecordsQueryDao, RecordAttsDao {

    private final BoardRecordsDao recordsDao;
    private final EcosTypeService ecosTypeService;

    @NotNull
    @Override
    public String getId() {
        return ResolvedBoardRecord.ID;
    }

    @Nullable
    @Override
    public Object queryRecords(@NotNull RecordsQuery recordsQuery) {
        RecsQueryRes<BoardWithMeta> queryRes = recordsDao.queryRecords(recordsQuery);
        if (queryRes != null) {
            queryRes.withRecords(record -> new ResolvedBoardRecord(record.getBoardDef(), ecosTypeService));
        }
        return queryRes;
    }

    @Nullable
    @Override
    public ResolvedBoardRecord getRecordAtts(@NotNull String localBoardId) {
        BoardWithMeta board = recordsDao.getRecordAtts(localBoardId);
        if (board != null) {
            return new ResolvedBoardRecord(board.getBoardDef(), ecosTypeService);
        }
        return null;
    }

}
