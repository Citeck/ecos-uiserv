package ru.citeck.ecos.uiserv.domain.dashdoard.repo;

import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import javax.persistence.*;
import java.util.Objects;

@Data
@Entity
@Table(name = "dashboards")
public class DashboardEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dashboards_seq_gen")
    @SequenceGenerator(name = "dashboards_seq_gen")
    private Long id;

    private String extId;
    private String typeRef;
    @ColumnTransformer(write = "LOWER(?)")
    private String authority;
    private String appliedToRef;
    @NotNull
    private String scope = "";
    private float priority;
    private byte[] config;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DashboardEntity that = (DashboardEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
