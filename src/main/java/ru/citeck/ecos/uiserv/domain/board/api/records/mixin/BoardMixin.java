package ru.citeck.ecos.uiserv.domain.board.api.records.mixin;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx;
import ru.citeck.ecos.records3.record.mixin.AttMixin;
import ru.citeck.ecos.uiserv.domain.board.api.records.BoardRecordsDao;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardWithMeta;
import ru.citeck.ecos.uiserv.domain.board.service.BoardService;
import ru.citeck.ecos.uiserv.domain.journal.api.records.ResolvedJournalRecordsDao;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
    private final WorkspaceService workspaceService;

    public BoardMixin(
        BoardService boardService,
        ResolvedJournalRecordsDao resolvedJournalRecordsDao,
        WorkspaceService workspaceService
    ) {
        this.workspaceService = workspaceService;
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
                return list.stream().map(board -> {
                    var boardLocalId = workspaceService.addWsPrefixToId(
                        board.getBoardDef().getId(),
                        board.getBoardDef().getWorkspace()
                    );
                    return EntityRef.create(AppName.UISERV, BoardRecordsDao.ID, boardLocalId);
                }).collect(Collectors.toList());
            default: {
                log.warn("Unpredictable attribute {}", path);
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
