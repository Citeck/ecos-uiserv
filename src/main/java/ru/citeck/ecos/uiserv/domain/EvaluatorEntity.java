package ru.citeck.ecos.uiserv.domain;

import lombok.Data;

import javax.persistence.*;
import java.util.Objects;

@Data
@Entity
@Table(name = "evaluators")
public class EvaluatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evaluators_seq_gen")
    @SequenceGenerator(name = "evaluators_seq_gen")
    protected Long id;

    @Column(name = "evaluator_id")
    private String evaluatorId;

    @Column(name = "type")
    private String type;

    @Lob
    @Column(name = "config_json")
    private String configJson;

    @Column(name = "inverse")
    private boolean inverse;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EvaluatorEntity that = (EvaluatorEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
