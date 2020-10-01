package ru.citeck.ecos.uiserv.domain.theme.repo;

import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Table(name = "ecos_theme")
@Getter
@Setter
public class ThemeEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @NotNull
    private String extId;
    @NotNull
    private String tenant;
    @NotNull
    private String images;
    @NotNull
    private String name;

    private byte[] resources;

    private String parentRef;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ThemeEntity themeEntity = (ThemeEntity) o;
        if (themeEntity.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), themeEntity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
