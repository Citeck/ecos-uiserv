package ru.citeck.ecos.uiserv.domain.icon.repo;

import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import javax.persistence.*;
import java.util.Objects;

@Data
@ToString(exclude = {"data"})
@Entity
@Table(name = "icon")
public class IconEntity extends AbstractAuditingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
    private Long id;
    @Column(name = "ext_id")
    private String extId;
    private String family;
    private String type;
    private String config;
    private byte[] data;

    @Column(name = "mimetype")
    private String mimeType;

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
