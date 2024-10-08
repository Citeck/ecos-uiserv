package ru.citeck.ecos.uiserv.domain.menu.repo;

import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ecos_menu")
@Getter
@Setter
public class MenuEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
    private Long id;
    @NotNull
    private String extId;
    @NotNull
    private String tenant;
    @NotNull
    private String type;
    @NotNull
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "menu_id")
    @Column(name = "authority")
    @CollectionTable(name = "ecos_menu_authority", joinColumns = @JoinColumn(name = "menu_id"))
    private List<String> authorities;
    @NotNull
    private float priority;
    @NotNull
    private String items;

    private Integer version;

    private String workspace;

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
