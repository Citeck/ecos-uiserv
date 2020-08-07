package ru.citeck.ecos.uiserv.domain.action.repo;

import lombok.Data;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;
import ru.citeck.ecos.uiserv.domain.evaluator.repo.EvaluatorEntity;

import javax.persistence.*;
import java.util.Objects;

@Data
@Entity
@Table(name = "actions")
public class ActionEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "actions_seq_gen")
    @SequenceGenerator(name = "actions_seq_gen")
    private Long id;

    @Column(name = "ext_id")
    private String extId;
    private String name;
    private String type;
    private String icon;

    @Lob
    @Column(name = "config_json")
    private String configJson;

    @OneToOne(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "evaluator")
    private EvaluatorEntity evaluator;

    private String confirm;
    private String result;
    private String features;
    private String pluralName;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActionEntity that = (ActionEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
