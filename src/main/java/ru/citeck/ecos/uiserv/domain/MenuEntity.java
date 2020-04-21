package ru.citeck.ecos.uiserv.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Table(name = "ecos_menu")
@Getter
@Setter
public class MenuEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @NotNull
    private String extId;
    @NotNull
    private String tenant;
    @NotNull
    private String type;
    @NotNull
    private String authorities;
    @NotNull
    private float priority;
    @NotNull
    private String items;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MenuEntity menuEntity = (MenuEntity) o;
        if (menuEntity.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), menuEntity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
