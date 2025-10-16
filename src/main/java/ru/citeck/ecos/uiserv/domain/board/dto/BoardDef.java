package ru.citeck.ecos.uiserv.domain.board.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;

@Data
public class BoardDef {

    public static final CardFieldsLabelLayout DEFAULT_CARD_FIELDS_LABEL_LAYOUT = CardFieldsLabelLayout.TOP;

    private String id;
    private String workspace;

    private EntityRef typeRef;
    private EntityRef journalRef;
    private EntityRef cardFormRef;

    private String cardTitleTemplate;

    private CardFieldsLabelLayout cardFieldsLabelLayout = DEFAULT_CARD_FIELDS_LABEL_LAYOUT;

    private Boolean readOnly;
    private Boolean disableTitle;

    private MLText name;
    private List<EntityRef> actions;
    private List<BoardColumnDef> columns;

    public BoardDef() {
    }

    public BoardDef(String id) {
        this.id = id;
    }

    public BoardDef(BoardDef other) {
        this.id = other.getId();
        this.workspace = other.getWorkspace();
        this.typeRef = other.getTypeRef();
        this.journalRef = other.getJournalRef();
        this.cardFormRef = other.getCardFormRef();
        this.cardTitleTemplate = other.getCardTitleTemplate();
        this.cardFieldsLabelLayout = other.getCardFieldsLabelLayout();
        this.readOnly = other.getReadOnly();
        this.disableTitle = other.getDisableTitle();
        this.name = other.getName();
        this.actions = other.getActions();
        this.columns = other.getColumns();
    }

    public EntityRef getTypeRef() {
        return typeRef;
    }

    public enum CardFieldsLabelLayout {
        /**
         * Indicates that the label should be displayed to the left of the input field.
         */
        LEFT,
        /**
         * Indicates that the label should be displayed above the input field.
         */
        TOP
    }
}
