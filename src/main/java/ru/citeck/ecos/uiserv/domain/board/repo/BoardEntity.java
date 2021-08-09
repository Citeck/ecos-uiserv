package ru.citeck.ecos.uiserv.domain.board.repo;

import lombok.Data;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Data
@Table(name = "board")
public class BoardEntity extends AbstractAuditingEntity {
    public static String ID = "id";
    public static String APP_NAME = "uiserv";
    public static String SOURCE_ID = "board";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "board_seq_gen")
    @SequenceGenerator(name = "board_seq_gen")
    private Long id;

    @NotNull
    @Column(name = "ext_id")
    private String extId;
    @Column(name = "type_ref")
    private String typeRef;
    @Column(name = "journal_ref")
    private String journalRef;
    @Column(name = "card_form_ref")
    private String cardFormRef;

    @Column(name = "read_only")
    private Boolean readOnly;

    @Column(name = "name")
    private String name;
    @Column(name = "actions")
    private String actions;
    @Column(name = "columns")
    private String columns;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BoardEntity that = (BoardEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
