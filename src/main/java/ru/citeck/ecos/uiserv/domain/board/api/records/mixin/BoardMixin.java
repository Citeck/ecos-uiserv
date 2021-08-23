package ru.citeck.ecos.uiserv.domain.board.api.records.mixin;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx;
import ru.citeck.ecos.records3.record.mixin.AttMixin;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.uiserv.domain.journal.api.records.ResolvedJournalRecordsDao;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BoardMixin implements AttMixin {

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

    private final BoardService boardService;

    public BoardMixin(BoardService boardService, ResolvedJournalRecordsDao resolvedJournalRecordsDao) {
        this.boardService = boardService;
        resolvedJournalRecordsDao.addAttributesMixin(this);
    }

    @Nullable
    @Override
    public Object getAtt(@NotNull String path, @NotNull AttValueCtx attValueCtx) throws Exception {
        Attributes attribute = Attributes.fromString(path);
        switch (attribute) {
            case BOARDS:
                 List<BoardWithMeta> list = boardService.getBoardsForJournal(attValueCtx.getLocalId());
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
        return Arrays.stream(Attributes.values())
            .map(attribute -> attribute.value)
            .collect(Collectors.toSet());
    }
}
