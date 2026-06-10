package ru.citeck.ecos.uiserv.domain.board.api.records;

import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnDef;
import ru.citeck.ecos.uiserv.domain.board.dto.BoardColumnFilters;

/**
 * One column of the resolved board, as exposed by the {@code rboard} source. Carries the stored
 * column fields plus the computed {@link #getAdditionalFilter() additionalFilter} predicate. The UI
 * reads {@code additionalFilter} and sends it back in the board-cards query, and the board-cards
 * backend applies it — keeping the per-column filtering rule (e.g. the {@code hideOldItems} cutoff)
 * in a single place.
 */
public class ResolvedBoardColumn {

    private final BoardColumnDef def;

    public ResolvedBoardColumn(BoardColumnDef def) {
        this.def = def;
    }

    public String getId() {
        return def.getId();
    }

    public MLText getName() {
        return def.getName();
    }

    public boolean getHideOldItems() {
        return def.getHideOldItems();
    }

    public String getHideItemsOlderThan() {
        return def.getHideItemsOlderThan();
    }

    public boolean getHasSum() {
        return def.getHasSum();
    }

    public String getSumAtt() {
        return def.getSumAtt();
    }

    public Predicate getAdditionalFilter() {
        return BoardColumnFilters.additionalFilter(def.getHideOldItems(), def.getHideItemsOlderThan());
    }
}
