package ru.citeck.ecos.uiserv.domain.board.repo;

import lombok.Data;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Data
@Table(name = "board")
public class BoardEntity extends AbstractAuditingEntity {

    public static String ID = "id";
    public static String SOURCE_ID = "board";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
    private Long id;

    @NotNull
    private String extId;
    private String typeRef;
    private String journalRef;
    private String cardFormRef;
    private String cardFieldsLabelLayout;

    private Boolean readOnly;
    private Boolean disableTitle;

    private String name;
    private String actions;
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
