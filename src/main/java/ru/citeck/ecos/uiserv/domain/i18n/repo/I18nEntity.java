package ru.citeck.ecos.uiserv.domain.i18n.repo;

import lombok.Data;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import javax.persistence.*;
import java.util.Objects;

@Data
@Entity
@Table(name = "internationalization")
public class I18nEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
    private Long id;

    @Column(name = "ext_id")
    private String extId;

    private String tenant;

    private String locales;
    private String messages;
    @Column(name = "module_order")
    private float order;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        I18nEntity that = (I18nEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
