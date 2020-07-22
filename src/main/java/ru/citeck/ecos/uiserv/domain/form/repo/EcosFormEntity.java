package ru.citeck.ecos.uiserv.domain.form.repo;

import lombok.Data;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import javax.persistence.*;
import java.util.Objects;

@Data
@Entity
@Table(name = "ecos_forms")
public class EcosFormEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ecos_forms_seq_gen")
    @SequenceGenerator(name = "ecos_forms_seq_gen")
    private Long id;

    @Column(name = "ext_id")
    private String extId;
    @Column(name = "type_ref")
    private String typeRef;

    private String title;
    private String description;
    @Column(name = "form_key")
    private String formKey;
    private String width;
    @Column(name = "custom_module")
    private String customModule;

    private String definition;
    private String i18n;
    private String attributes;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EcosFormEntity that = (EcosFormEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
