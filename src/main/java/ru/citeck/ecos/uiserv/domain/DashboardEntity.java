package ru.citeck.ecos.uiserv.domain;

import lombok.Data;

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

    @Column(name = "ext_id")
    private String extId;
    private String typeRef;
    private String authority;
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
