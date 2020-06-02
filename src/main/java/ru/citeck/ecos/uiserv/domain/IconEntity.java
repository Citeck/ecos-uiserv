package ru.citeck.ecos.uiserv.domain;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.Objects;

@Data
@ToString(exclude = {"data"})
@Entity
@Table(name = "icon")
public class IconEntity extends AbstractAuditingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "icon_seq_gen")
    @SequenceGenerator(name = "icon_seq_gen")
    private Long id;
    @Column(name = "ext_id")
    private String extId;
    private String family;
    private String type;
    private String config;
    private byte[] data;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IconEntity that = (IconEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
