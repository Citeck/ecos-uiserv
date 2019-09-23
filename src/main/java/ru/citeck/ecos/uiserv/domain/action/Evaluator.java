package ru.citeck.ecos.uiserv.domain.action;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * @author Roman Makarskiy
 */
@Data
@Entity
@EqualsAndHashCode
@Table(name = "evaluators")
public class Evaluator {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    protected Long id;

    @Column(name = "evaluator_id")
    private String evaluatorId;

    @Lob
    @Column(name = "config_json")
    private String configJSON;

}
