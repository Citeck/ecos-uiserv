package ru.citeck.ecos.uiserv.domain.board.api.records.mixin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx;
import ru.citeck.ecos.records3.record.mixin.AttMixin;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.repo.BoardEntity;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.uiserv.domain.journal.api.records.ResolvedJournalRecordsDao;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class BoardMixin implements AttMixin {
    private static final Logger log = LoggerFactory.getLogger(BoardMixin.class);

    enum Attributes {
        BOARDS("boardRefs");
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

    final BoardService boardService;
    final ResolvedJournalRecordsDao resolvedJournalRecordsDao;

    public BoardMixin(BoardService boardService, ResolvedJournalRecordsDao resolvedJournalRecordsDao) {
        this.boardService = boardService;
        this.resolvedJournalRecordsDao = resolvedJournalRecordsDao;
        resolvedJournalRecordsDao.addAttributesMixin(this);
    }

    @Nullable
    @Override
    public Object getAtt(@NotNull String path, @NotNull AttValueCtx attValueCtx) throws Exception {
        RecordRef journalRef = RecordRef.create(BoardEntity.APP_NAME, "journal", attValueCtx.getLocalId());
        Attributes attribute = Attributes.fromString(path);
        switch (attribute) {
            case BOARDS:
                 List<BoardWithMeta> list = boardService.getBoardsForJournal(journalRef);
                 return list.stream().map(BoardWithMeta::getRef).collect(Collectors.toList());
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
