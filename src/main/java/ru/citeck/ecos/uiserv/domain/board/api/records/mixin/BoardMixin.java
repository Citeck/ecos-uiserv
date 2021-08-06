package ru.citeck.ecos.uiserv.domain.board.api.records.mixin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx;
import ru.citeck.ecos.records3.record.mixin.AttMixin;
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao;
import ru.citeck.ecos.uiserv.domain.board.api.records.ResolvedBoardRecord;
import ru.citeck.ecos.uiserv.domain.board.api.records.ResolvedBoardRecordsDao;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardDef;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class BoardMixin implements AttMixin {
    private static final Logger log = LoggerFactory.getLogger(BoardMixin.class);

    enum Attributes {
        JOURNAL("journal"),
        LOCAL_ID("localId"),
        EXT_ID("extId"),
        MODULE_ID("moduleId"),
        READ_ONLY("readOnly"),
        COLUMNS("columns"),
        CARD_FORM("cardForm"),
        TYPE_REF("typeRef");
        private final String value;

        Attributes(String value) {
            this.value = value;
        }

        static Attributes fromString(String value) throws IllegalArgumentException {
            return Arrays.stream(Attributes.values())
                .filter(attribute -> attribute.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid attribute value '%s'", value)));
        }
    }

    final BoardRecordsDao boardRecordsDao;
    final ResolvedBoardRecordsDao resolvedBoardRecordsDao;

    public BoardMixin(BoardRecordsDao boardRecordsDao, ResolvedBoardRecordsDao resolvedBoardRecordsDao) {
        this.boardRecordsDao = boardRecordsDao;
        this.resolvedBoardRecordsDao = resolvedBoardRecordsDao;
        boardRecordsDao.addAttributesMixin(this);
        resolvedBoardRecordsDao.addAttributesMixin(this);
    }

    @Nullable
    @Override
    public Object getAtt(@NotNull String path, @NotNull AttValueCtx attValueCtx) throws Exception {
        ResolvedBoardRecord resolvedBoardRecord = resolvedBoardRecordsDao.getRecordAtts(attValueCtx.getLocalId());
        if (resolvedBoardRecord == null || resolvedBoardRecord.getBoardDef() == null) {
            return null;
        }
        BoardDef boardDef = resolvedBoardRecord.getBoardDef();
        Attributes attribute = Attributes.fromString(path);
        switch (attribute) {
            case EXT_ID:
            case LOCAL_ID:
            case MODULE_ID:
                return boardDef.getId();
            case TYPE_REF:
                return resolvedBoardRecord.getTypeRef();
            case COLUMNS:
                return resolvedBoardRecord.getColumns();
            case JOURNAL:
                return boardDef.getJournalRef();
            case READ_ONLY:
                return boardDef.getReadOnly();
            case CARD_FORM:
                return resolvedBoardRecord.getCardFormRef();
            default: {
                log.warn("Unpredictable attribute " + path);
                return null;
            }
        }
    }

    @NotNull
    @Override
    public Collection<String> getProvidedAtts() {
        return Arrays.stream(Attributes.values()).map(attribute -> attribute.value).collect(Collectors.toSet());
    }
}
